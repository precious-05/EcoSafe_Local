
<div align="center">

# EcoSafe AI
> **Forest Fire Detection & Intelligent Risk Analysis System**

[![Java](https://img.shields.io/badge/Java-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.95+-009688?style=for-the-badge&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![TensorFlow](https://img.shields.io/badge/TensorFlow-Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
[![Python](https://img.shields.io/badge/Python-3.9+-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://www.python.org/)

[![Status](https://img.shields.io/badge/Status-Completed-brightgreen?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

</div>

---

EcoSafe AI is a state-of-the-art, full-stack application designed to detect forest fires in real-time using custom Machine Learning models and provide intelligent threat analysis. By combining a high-performance **FastAPI (Python)** backend running a specialized **TensorFlow Lite** model with a premium, interactive **Java Android mobile client**, EcoSafe AI empowers tourists, foresters, and environmentalists to scan forest terrains, identify fire outbreaks, analyze visual risk metrics, and report incidents instantly to emergency departments.

---

## Table of Contents
- [About the App](#about-the-app)
- [App Screenshots](#app-screenshots)
- [Key Features](#key-features)
- [Technologies Used](#technologies-used)
- [APK Download](#apk-download)
- [How to Install the APK](#how-to-install-the-apk)
- [How to Run the Project](#how-to-run-the-project)
- [Privacy Policy](#privacy-policy)
- [Future Enhancements](#future-enhancements)
- [Developed By](#developed-by)

---

## About the App

EcoSafe AI addresses the critical problem of rapid wildfire escalation through early visual detection and automated geo-reporting.

| Aspect | Description |
|:-------|:------------|
| **What it is** | A comprehensive forest fire diagnostics and risk evaluation application powered by local TensorFlow Lite classification models |
| **Who can use it** | Tourists, hikers, forest authorities, researchers, and local residents inhabiting regions vulnerable to forest fire hazards |
| **Problem it solves** | Wildfires often spread undetected in remote forest regions due to delayed reporting. EcoSafe AI enables instant, on-site image analysis, generates GPS-tagged fire reports with confidence scores, and maps fire activity |
| **Main features** | Real-time CameraX scanning, automated SQLite database synchronization, dynamic interactive data visualization charts, hybrid map tracking with fire threat buffers, a comprehensive local directory of vulnerable forests, and a one-tap emergency calling & location sharing suite |

---

## App Screenshots

### Onboarding & Permissions

| Splash Screen | Welcome Screen | Location Permission | Camera Permission |
| :---: | :---: | :---: | :---: |
| ![Splash Screen](screenshots/ss1.png) | ![Welcome Screen](screenshots/ss4.png) | ![Location Permission](screenshots/ss2.png) | ![Camera Permission](screenshots/ss3.png) |

### AI Fire Detection

| Detection | Detection Result | Detection Result | Detection Result | Detection Result |
| :---: | :---: | :---: | :---: | :---: |
| ![Detection](screenshots/ss8.png) | ![Detection Result](screenshots/ss10.png) | ![Detection Result](screenshots/ss11.png) | ![Detection Result](screenshots/ss15.png) | ![Detection Result](screenshots/ss18.png) |

### Incident Monitoring

| Incident Details | Incident Details |
| :---: | :---: |
| ![Incident Details](screenshots/ss12.png) | ![Incident Details](screenshots/ss16.png) |

### Risk Analysis & Dashboard

| Risk Analysis | Dashboard | Risk Analysis |
| :---: | :---: | :---: |
| ![Risk Analysis](screenshots/ss19.png) | ![Dashboard](screenshots/ss21.png) | ![Risk Analysis](screenshots/ss24.png) |

### Emergency Response

| Emergency Portal |
| :---: |
| <img src="screenshots/ss20.png" width="300" height="450"/> |

### Forest Intelligence

| High-Risk Forests | Medium-Risk Forests | Forest Information |
| :---: | :---: | :---: |
| ![Forest Information](screenshots/ss22.png) | ![Forest Information](screenshots/ss23.png) | ![Forest Information](screenshots/ss25.png) |

---

## Key Features

### 1. Intelligent Real-Time Fire Scan
- **CameraX Integration:** Capture live images directly within the app using a high-performance, low-latency camera interface, or select photos from your device gallery
- **Instant ML Diagnostics:** Upload captured images to the FastAPI server for binary classification (Fire vs. Safe Scan) processed via a specialized **TensorFlow Lite (`f.tflite`)** model
- **Auditory Alarms:** Triggers a looping high-volume alert sound locally on the device immediately upon detecting fire to alert the user of immediate danger

### 2. Dynamic Risk Analytics Dashboard
- **Dynamic Threat Gauge:** Automatically evaluates fire threat level (LOW, MEDIUM, HIGH) based on historical local log statistics
- **Rich Data Visualization:** Uses interactive charts to render:
  - **Fire vs. Safe Distribution:** Pie chart showing fire outbreaks against safe scans
  - **7-Day Scan Activity:** Bar chart mapping daily diagnostic frequency
  - **Detection Confidence Trend:** Line chart tracking model classification confidence scores
  - **Incident Distribution by Zone:** Horizontal bar chart showing fire outbreaks grouped by geographical sectors

### 3. Interactive Threat Map
- **Hybrid Map Visualization:** Integrates hybrid view to trace all local scans
- **Visual Geo-tagging:** Color-coded markers on scan coordinates – Orange for fire outbreaks, Green for safe scans
- **Threat Buffer Rings:** Dynamic translucent orange threat circles around clustered fire locations

### 4. Vulnerable Forest Safety Directory
- **National Forest Profiles:** Database of vulnerable forest areas in Pakistan (Margalla Hills, Murree Forest, Swat Forest, Ziarat Forest, Kotli Sattian, Abbottabad, and more)
- **Vegetation & Fire Season Profiles:** Displays tree varieties (Chir Pine, Deodar, Oak), peak fire risk months (April-June), and ecological details
- **Actionable Safety Guides:** Specialized precautions to prevent accidental wildfire ignitions
- **Bottom Sheet Modals:** Interactive, animated card-based modals for forest summaries

### 5. Instant Emergency Portal
- **Hotline Dispatcher:** Speed-dial buttons to instantly call Rescue 1122, Police 15, and Forest Department 1084
- **Telemetry Sharing:** One-click utility to copy current GPS coordinates (Latitude & Longitude) or share a location URL via messaging/social apps

### 6. Offline-First & Automatic Cloud Sync
- **Local Database Store:** Logs all diagnostics to local SQLite database (`incidents.db`) for full functionality in remote forest regions without internet
- **Dynamic Synchronizer:** Connects to FastAPI backend to sync local data with cloud server database (`forest_fire.db`)
- **Interactive Logs Management:** Swipe-to-delete, filter logs by timeframe (7 Days, 30 Days, All), search by coordinates, or clear database

---

## Technologies Used

### Android Mobile Client (Frontend)

| Technology |       |
|:-----------|:------|
| **Language:** Java | [![Java](https://img.shields.io/badge/Java-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/) |
| **IDE:** Android Studio | [![Android Studio](https://img.shields.io/badge/Android_Studio-3DDC84?style=flat-square&logo=android-studio&logoColor=white)](https://developer.android.com/studio) |
| **Camera:** CameraX API | [![CameraX](https://img.shields.io/badge/CameraX-Jetpack-1E88E5?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/camerax) |
| **Location:** FusedLocationProviderClient | [![Google Maps](https://img.shields.io/badge/Location-Google_Services-4285F4?style=flat-square&logo=google&logoColor=white)](https://developers.google.com/maps) |
| **Maps:** Google Maps SDK | [![Google Maps](https://img.shields.io/badge/Maps-Google_SDK-4285F4?style=flat-square&logo=googlemaps&logoColor=white)](https://developers.google.com/maps) |
| **Charts:** MPAndroidChart | [![MPAndroidChart](https://img.shields.io/badge/Charts-MPAndroidChart-FF6F00?style=flat-square)](https://github.com/PhilJay/MPAndroidChart) |
| **Networking:** Retrofit 2 + OkHttp3 | [![Retrofit](https://img.shields.io/badge/Networking-Retrofit-48B983?style=flat-square)](https://square.github.io/retrofit/) |
| **Permissions:** Dexter | [![Dexter](https://img.shields.io/badge/Permissions-Dexter-673AB7?style=flat-square)](https://github.com/Karumi/Dexter) |

### FastAPI Services (Backend)

| Technology |       |
|:-----------|:------|
| **Language:** Python | [![Python](https://img.shields.io/badge/Python-3.9+-3776AB?style=flat-square&logo=python&logoColor=white)](https://www.python.org/) |
| **Framework:** FastAPI | [![FastAPI](https://img.shields.io/badge/FastAPI-0.95+-009688?style=flat-square&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/) |
| **ML Engine:** TensorFlow Lite | [![TensorFlow](https://img.shields.io/badge/TensorFlow-Lite-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite) |
| **Database:** SQLite3 | [![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white)](https://www.sqlite.org/) |
| **Image Processing:** Pillow, NumPy | [![Pillow](https://img.shields.io/badge/Image-Pillow-3E7E9E?style=flat-square&logo=python&logoColor=white)](https://python-pillow.org/) |
| **ASGI Server:** Uvicorn | [![Uvicorn](https://img.shields.io/badge/Server-Uvicorn-3E7E9E?style=flat-square)](https://www.uvicorn.org/) |

---

## APK Download

The compiled debug APK is located in the build directory after compilation:

[Download EcoSafe AI Debug APK](file:///e:/EcoSafe_Local/EcoSafe%20AI/app/build/outputs/apk/debug/app-debug.apk)

---

## How to Install the APK

1. **Download the APK:** Click the link above to download the `EcoSafe_AI.apk` file
2. **Transfer to Device:** Copy the downloaded APK to your Android smartphone (if downloaded on PC)
3. **Enable Unknown Sources:** Go to `Settings > Security` (or `Apps > Special Access`) and allow installation from **Unknown Sources**
4. **Install & Launch:** Open your device's File Manager, tap on the APK file, click **Install**, and launch **EcoSafe AI**

---

## How to Run the Project

### 1. Setting Up the FastAPI Backend Server

#### Prerequisites
- Python 3.9 or higher installed
- SQLite3 installed

#### Steps

```bash
# Navigate to backend folder
cd backend

# Install dependencies
pip install fastapi uvicorn tensorflow numpy pillow requests

# Ensure these files are in backend/ directory:
# - f.tflite (TensorFlow Lite model)
# - alarm.wav (alert sound)

# Launch the server
python main.py
```

The server will start at: `http://localhost:8000`

- API Documentation: `http://localhost:8000/docs`
- Health Check: `http://localhost:8000/health`

> [!NOTE]
> Ensure your computer and Android device are on the same Wi-Fi network. The mobile app features a dynamic configuration screen where you can save your server URL at runtime - no Java code modification needed.

---

### 2. Running the Android Application in Android Studio

1. Open **Android Studio**
2. Select **Open an Existing Project** and choose the `EcoSafe AI` directory
3. Wait for indexing and **Sync Gradle files**
4. Configure **Google Maps API Key** in `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```
5. **Configure Backend URL:** Install the APK, tap the Settings icon, enter your computer's local IP address (e.g., 192.168.1.100:8000), and tap Save
6. Connect an Android emulator or physical device with USB Debugging enabled
7. Click the **Run** button to build and install the app

---

## Privacy Policy

EcoSafe AI values user privacy. The application accesses device location and camera exclusively to detect and report environmental wildfire threats. No personal datasets are shared with unverified external third-party servers.

**View Published Privacy Policy:** [https://ecosafe-privacy-police.netlify.app/](https://ecosafe-privacy-police.netlify.app/)

---

## Future Enhancements

- **Push Notification Alerts:** Firebase Cloud Messaging (FCM) to alert residents within 5km of a verified forest fire
- **Deep Meteorological Risk Forecasting:** Real-time weather APIs for wind speed, humidity, and temperature risk calculations
- **Advanced Admin Management Portal:** Web-based panel to view heatmaps, dispatch response units, and resolve incidents
- **On-Device Offline Inference:** Embed TensorFlow Lite model directly into the Android app for offline fire classification

---

## Developed By

| | |
|:---|:---|
| **Developer Name** | Alina Liaquat |
| **GitHub** | [@precious-05](https://github.com/precious-05) |
| **Email** | [alina.insights@gmail.com](mailto:alina.insights@gmail.com) |
| **Class & Semester** | BS Computer Science - 6th Semester |
| **Department** | Department of Computer Science |
| **LinkedIn** | [www.linkedin.com/in/alina-liaquat-779347325](https://www.linkedin.com/in/alina-liaquat-779347325) |

---

<div align="center">

**EcoSafe AI - Protecting our forests, preserving our future**

</div>


**Summary:** Font-awesome GitHub mein kaam nahi karta. Isliye maine **badges (shields.io)** use kiye hain - ye professional lagte hain aur har jagah render hote hain. Content 100% same hai, sirf icons replace kiye hain.
