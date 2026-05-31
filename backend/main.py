import os
import sqlite3
import warnings
import uvicorn
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, UploadFile, File, HTTPException, Request, Form
from typing import Optional
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import io
import logging
from datetime import datetime
from contextlib import contextmanager

# Suppress warnings and TensorFlow logs
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
warnings.filterwarnings('ignore')

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="EcoSafe-AI Forest Fire Detection API",
    description="API for detecting forest fires using TensorFlow Lite model",
    version="1.0.0"
)

# Add CORS middleware for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
MODEL_PATH = "f.tflite"
DATABASE_PATH = "forest_fire.db"
SOUND_PATH = "alarm.wav"
THRESHOLD = 0.5
TARGET_SIZE = (128, 128)

# Global variables for model
interpreter = None
input_details = None
output_details = None


# Database helper functions
@contextmanager
def get_db_connection():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()


def init_database():
    """Create database tables if they don't exist"""
    with get_db_connection() as conn:
        cursor = conn.cursor()
        
        # Create incidents table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS incidents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image_path TEXT,
                confidence REAL,
                latitude REAL,
                longitude REAL,
                timestamp TEXT,
                status TEXT DEFAULT 'reported',
                user_id TEXT DEFAULT 'public',
                synced INTEGER DEFAULT 1
            )
        ''')
        
        # Create pending_reports table for offline sync
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS pending_reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image_path TEXT,
                latitude REAL,
                longitude REAL,
                timestamp TEXT,
                confidence REAL,
                retry_count INTEGER DEFAULT 0
            )
        ''')
        
        # Create users table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                email TEXT UNIQUE,
                role TEXT DEFAULT 'public',
                phone TEXT,
                created_at TEXT
            )
        ''')
        
        conn.commit()
        logger.info("Database initialized successfully")


def current_timestamp():
    """Consistent timestamp format matching the Android app."""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def save_incident(
    image_path,
    confidence,
    latitude,
    longitude,
    user_id="public",
    status="reported",
    timestamp=None,
):
    """Save a fire incident to database"""
    with get_db_connection() as conn:
        cursor = conn.cursor()
        if timestamp is None:
            timestamp = current_timestamp()
        
        cursor.execute('''
            INSERT INTO incidents (image_path, confidence, latitude, longitude, timestamp, status, user_id, synced)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (image_path, confidence, latitude, longitude, timestamp, status, user_id, 1))
        
        conn.commit()
        return cursor.lastrowid


def get_all_incidents(limit=100):
    """Get all incidents from database"""
    with get_db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, image_path, confidence, latitude, longitude, timestamp, status, user_id
            FROM incidents 
            ORDER BY timestamp DESC 
            LIMIT ?
        ''', (limit,))
        
        return [dict(row) for row in cursor.fetchall()]


def get_incidents_by_date(start_date, end_date):
    """Get incidents within date range"""
    with get_db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, image_path, confidence, latitude, longitude, timestamp, status, user_id
            FROM incidents 
            WHERE timestamp BETWEEN ? AND ?
            ORDER BY timestamp DESC
        ''', (start_date, end_date))
        
        return [dict(row) for row in cursor.fetchall()]


# Model functions
def load_model():
    """Load the TFLite model"""
    global interpreter, input_details, output_details
    
    try:
        if not os.path.exists(MODEL_PATH):
            logger.error(f"Model file not found at {MODEL_PATH}")
            return False
        
        interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        logger.info("Model loaded successfully")
        logger.info(f"Input shape: {input_details[0]['shape']}")
        logger.info(f"Output shape: {output_details[0]['shape']}")
        
        return True
        
    except Exception as e:
        logger.error(f"Error loading model: {e}")
        return False


def preprocess_image(image_bytes: bytes) -> np.ndarray:
    """
    Preprocess image for TFLite model
    Resize to 128x128, normalize to 0-1, add batch dimension
    """
    try:
        image = Image.open(io.BytesIO(image_bytes))
        
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        image = image.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
        
        image_array = np.array(image, dtype=np.float32)
        image_array = image_array / 255.0
        
        image_array = np.expand_dims(image_array, axis=0)
        
        expected_shape = (1, 128, 128, 3)
        if image_array.shape != expected_shape:
            raise ValueError(f"Unexpected shape: {image_array.shape}, expected: {expected_shape}")
        
        return image_array
        
    except Exception as e:
        logger.error(f"Error in preprocessing: {e}")
        raise HTTPException(status_code=400, detail=f"Image preprocessing failed: {str(e)}")


def predict_fire(image_array: np.ndarray) -> dict:
    """
    Run prediction using TFLite model
    Returns prediction result with confidence
    """
    try:
        interpreter.set_tensor(input_details[0]['index'], image_array)
        interpreter.invoke()
        
        prediction = interpreter.get_tensor(output_details[0]['index'])
        
        fire_probability = float(prediction[0][0] if prediction.ndim > 1 else prediction[0])
        
        is_fire = fire_probability > THRESHOLD
        result_label = "Fire" if is_fire else "Non-Fire"
        
        if is_fire:
            confidence = fire_probability * 100
        else:
            confidence = (1 - fire_probability) * 100
        
        return {
            "is_fire": is_fire,
            "result": result_label,
            "fire_probability": round(fire_probability, 4),
            "confidence_percentage": round(confidence, 2),
            "threshold_used": THRESHOLD
        }
        
    except Exception as e:
        logger.error(f"Error during prediction: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


# Startup event
@app.on_event("startup")
async def startup_event():
    logger.info("Starting EcoSafe-AI Forest Fire Detection API...")
    init_database()
    if not load_model():
        logger.warning("Model not loaded. API will not function properly.")
    else:
        logger.info("API ready for requests")
        if os.path.exists(SOUND_PATH):
            logger.info("Alert sound file found")
        else:
            logger.warning(f"Alert sound file not found at {SOUND_PATH}")


# API Endpoints
@app.get("/")
async def root():
    return {
        "name": "EcoSafe-AI Forest Fire Detection API",
        "version": "1.0.0",
        "status": "running",
        "model_loaded": interpreter is not None,
        "database": DATABASE_PATH,
        "endpoints": {
            "/predict": "POST - Upload image for fire detection",
            "/health": "GET - Check API health",
            "/model-info": "GET - Get model information",
            "/incidents": "GET - Get all fire incidents",
            "/incidents": "POST - Create new incident",
            "/sound": "GET - Download alert sound",
            "/privacy-policy": "GET - Privacy policy page"
        }
    }


@app.get("/health")
async def health_check():
    return {
        "status": "healthy" if interpreter is not None else "degraded",
        "model_loaded": interpreter is not None,
        "database_connected": os.path.exists(DATABASE_PATH),
        "model_path": MODEL_PATH if os.path.exists(MODEL_PATH) else "not found"
    }


@app.get("/model-info")
async def model_info():
    if interpreter is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    return {
        "input_shape": input_details[0]['shape'].tolist(),
        "input_dtype": str(input_details[0]['dtype']),
        "output_shape": output_details[0]['shape'].tolist(),
        "output_dtype": str(output_details[0]['dtype']),
        "threshold": THRESHOLD,
        "image_size": TARGET_SIZE,
        "normalization": "divide by 255.0"
    }


@app.get("/sound")
async def get_sound():
    """Download alert sound file"""
    if not os.path.exists(SOUND_PATH):
        raise HTTPException(status_code=404, detail="Sound file not found")
    return FileResponse(SOUND_PATH, media_type="audio/wav", filename="alarm.wav")


@app.get("/incidents")
async def get_incidents(limit: int = 100, start_date: str = None, end_date: str = None):
    """
    Get fire incidents from database
    - limit: max number of records (default 100)
    - start_date: filter by start date (ISO format)
    - end_date: filter by end date (ISO format)
    """
    try:
        if start_date and end_date:
            incidents = get_incidents_by_date(start_date, end_date)
        else:
            incidents = get_all_incidents(limit)
        
        return {
            "success": True,
            "total": len(incidents),
            "incidents": incidents
        }
    except Exception as e:
        logger.error(f"Error getting incidents: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/incidents")
async def create_incident(request: Request):
    """
    Create a new incident (POST request from Android app)
    Accepts JSON body with incident details
    """
    try:
        # Parse JSON body
        body = await request.json()
        
        image_path = body.get("image_path", "android_report")
        confidence = body.get("confidence", 0.0)
        latitude = body.get("latitude", 0.0)
        longitude = body.get("longitude", 0.0)
        timestamp = body.get("timestamp", current_timestamp())
        status = body.get("status", "reported")
        user_id = body.get("user_id", "android_user")
        
        # Save to database
        incident_id = save_incident(
            image_path=image_path,
            confidence=confidence,
            latitude=latitude,
            longitude=longitude,
            user_id=user_id,
            status=status,
            timestamp=timestamp,
        )
        
        logger.info(f"Incident created with ID: {incident_id}")
        
        return {
            "success": True,
            "incident_id": incident_id,
            "message": "Incident saved successfully"
        }
        
    except Exception as e:
        logger.error(f"Error creating incident: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/privacy-policy")
async def privacy_policy():
    """Serve the EcoSafe-AI privacy policy page."""
    policy_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "privacy_policy.html")
    if not os.path.exists(policy_path):
        raise HTTPException(status_code=404, detail="Privacy policy not found")
    return FileResponse(policy_path, media_type="text/html")


@app.post("/predict")
async def predict(
    file: UploadFile = File(...),
    latitude: Optional[float] = Form(default=None),
    longitude: Optional[float] = Form(default=None),
    user_id: str = Form(default="public"),
):
    """
    Predict if an image contains fire or not
    
    - Upload an image file
    - Optional: provide GPS coordinates
    - Returns fire detection result with confidence
    - Saves to database if fire detected
    """
    if interpreter is None:
        raise HTTPException(status_code=503, detail="Model not loaded. Please check server logs.")
    
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image (JPEG, PNG, etc.)")
    
    MAX_SIZE = 10 * 1024 * 1024
    
    try:
        image_bytes = await file.read()
        if len(image_bytes) > MAX_SIZE:
            raise HTTPException(status_code=400, detail=f"Image too large. Max size: 10MB")
        
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Empty file")
        
        processed_image = preprocess_image(image_bytes)
        prediction_result = predict_fire(processed_image)
        
        incident_id = None
        if prediction_result["is_fire"]:
            incident_id = save_incident(
                image_path=file.filename,
                confidence=prediction_result["confidence_percentage"],
                latitude=latitude if latitude else 0.0,
                longitude=longitude if longitude else 0.0,
                user_id=user_id
            )
            logger.info(f"Fire incident saved with ID: {incident_id}")
        
        response = {
            **prediction_result,
            "filename": file.filename,
            "content_type": file.content_type,
            "message": "Fire detected. Stay safe and alert authorities." if prediction_result["is_fire"] else "No fire detected. Forest is safe.",
            "incident_id": incident_id,
            "location_received": latitude is not None and longitude is not None
        }
        
        logger.info(f"Prediction completed for {file.filename}: {prediction_result['result']} - {prediction_result['confidence_percentage']}%")
        
        return JSONResponse(content=response)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@app.post("/predict-batch")
async def predict_batch(
    files: list[UploadFile] = File(...),
    latitude: Optional[float] = Form(default=None),
    longitude: Optional[float] = Form(default=None),
    user_id: str = Form(default="public"),
):
    """
    Predict for multiple images (max 10 images)
    """
    if interpreter is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    if len(files) > 10:
        raise HTTPException(status_code=400, detail="Maximum 10 images per batch request")
    
    results = []
    fire_count = 0
    
    for file in files:
        try:
            if not file.content_type or not file.content_type.startswith("image/"):
                results.append({
                    "filename": file.filename,
                    "error": "Invalid file type"
                })
                continue
            
            image_bytes = await file.read()
            processed_image = preprocess_image(image_bytes)
            prediction = predict_fire(processed_image)
            
            if prediction["is_fire"]:
                fire_count += 1
                save_incident(
                    image_path=file.filename,
                    confidence=prediction["confidence_percentage"],
                    latitude=latitude if latitude else 0.0,
                    longitude=longitude if longitude else 0.0,
                    user_id=user_id
                )
            
            results.append({
                "filename": file.filename,
                **prediction
            })
            
        except Exception as e:
            results.append({
                "filename": file.filename,
                "error": str(e)
            })
    
    return JSONResponse(content={
        "total_images": len(files),
        "fire_detected_count": fire_count,
        "results": results
    })


@app.post("/sync-pending")
async def sync_pending_reports(
    reports: list,
    user_id: str = "public"
):
    """
    Sync pending reports from offline mode
    Expects list of reports with: image_path, latitude, longitude, confidence
    """
    synced_count = 0
    failed_count = 0
    
    for report in reports:
        try:
            save_incident(
                image_path=report.get("image_path", "unknown"),
                confidence=report.get("confidence", 0),
                latitude=report.get("latitude", 0),
                longitude=report.get("longitude", 0),
                user_id=user_id
            )
            synced_count += 1
        except Exception as e:
            logger.error(f"Failed to sync report: {e}")
            failed_count += 1
    
    return {
        "synced_count": synced_count,
        "failed_count": failed_count,
        "message": f"Successfully synced {synced_count} reports"
    }


@app.delete("/incidents")
async def clear_all_incidents(admin_key: str = None):
    """
    Delete all incidents from the server database (admin only).
    Use when resetting test/demo data.
    """
    ADMIN_SECRET = "EcoSafe_Admin_2024"

    if admin_key != ADMIN_SECRET:
        raise HTTPException(status_code=403, detail="Unauthorized. Admin access required.")

    with get_db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM incidents")
        deleted = cursor.rowcount
        conn.commit()

    logger.info(f"Cleared all incidents from server (rows affected: {deleted})")
    return {
        "success": True,
        "message": "All incidents deleted from server",
        "deleted_count": deleted,
    }


@app.delete("/incidents/{incident_id}")
async def delete_incident(incident_id: int, admin_key: str = None):
    """
    Delete an incident by ID (admin only)
    Pass admin_key = "admin_secret_key" for security
    """
    ADMIN_SECRET = "EcoSafe_Admin_2024"
    
    if admin_key != ADMIN_SECRET:
        raise HTTPException(status_code=403, detail="Unauthorized. Admin access required.")
    
    with get_db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM incidents WHERE id = ?", (incident_id,))
        conn.commit()
        
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Incident not found")
        
        return {"message": "Incident deleted successfully", "incident_id": incident_id}


if __name__ == "__main__":
    print("\n" + "="*50)
    print("ECOSAFE-AI FOREST FIRE DETECTION API")
    print("="*50)
    print("\nModel Requirements:")
    print("   - Input shape: (1, 128, 128, 3)")
    print("   - Output: Single float (0-1)")
    print("   - Threshold: 0.5")
    print("\nFiles needed in same directory:")
    print("   - f.tflite (your trained model)")
    print("   - alarm.wav (alert sound)")
    print("\nStarting server...")
    print(" Local URL: http://localhost:8000")
    print(" API Docs: http://localhost:8000/docs")
    print("\n" + "="*50 + "\n")
    
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        reload=False,
        log_level="info"
    )