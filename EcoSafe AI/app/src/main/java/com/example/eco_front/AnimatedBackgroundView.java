package com.example.eco_front;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedBackgroundView extends View {

    private Paint backgroundPaint;
    private Paint starPaint;
    private Paint starGlowPaint;
    private Paint nebulaPaint;
    
    private List<Star> stars;
    private List<Nebula> nebulas;
    private Random random;
    private long lastTime;
    private float globalTime; // running timer for sine wave effects
    
    // Gradient colors
    private int startColor;
    private int midColor;
    private int endColor;

    private static class Star {
        float x;
        float y;
        float radius;
        float speed;
        float baseAlpha;
        float twinkleSpeed;
        float twinklePhase; // offset for organic twinkle variance
        int color;
    }

    private static class Nebula {
        float x;
        float y;
        float radius;
        float vx; // velocity x
        float vy; // velocity y
        int color;
        float maxAlpha;
    }

    public AnimatedBackgroundView(Context context) {
        super(context);
        init(context, null);
    }

    public AnimatedBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AnimatedBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        random = new Random();
        stars = new ArrayList<>();
        nebulas = new ArrayList<>();
        globalTime = random.nextFloat() * 1000f; // randomize start phase

        // Load colors from resources
        startColor = ContextCompat.getColor(context, R.color.pastel_bg_start);
        midColor = ContextCompat.getColor(context, R.color.pastel_bg_mid);
        endColor = ContextCompat.getColor(context, R.color.pastel_bg_end);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);

        starPaint = new Paint();
        starPaint.setAntiAlias(true);
        starPaint.setStyle(Paint.Style.FILL);

        starGlowPaint = new Paint();
        starGlowPaint.setAntiAlias(true);
        starGlowPaint.setStyle(Paint.Style.FILL);

        nebulaPaint = new Paint();
        nebulaPaint.setAntiAlias(true);
        nebulaPaint.setStyle(Paint.Style.FILL);

        lastTime = System.currentTimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Create shader for background gradient
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, h,
                new int[]{startColor, midColor, endColor},
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        backgroundPaint.setShader(gradient);

        // Generate stars
        stars.clear();
        int starCount = 65; // Balanced for premium aesthetics and performance
        
        int[] starColors = {
            0xFFFFFFFF, // Pure white
            ContextCompat.getColor(getContext(), R.color.pastel_green_soft),
            ContextCompat.getColor(getContext(), R.color.pastel_orange_pale),
            ContextCompat.getColor(getContext(), R.color.golden_accent)
        };

        for (int i = 0; i < starCount; i++) {
            Star star = new Star();
            star.x = random.nextFloat() * w;
            star.y = random.nextFloat() * h;
            star.radius = 1.5f + random.nextFloat() * 4.5f; // elegant size between 1.5dp and 6dp
            star.speed = 8 + random.nextFloat() * 22; // smooth drift speed
            star.baseAlpha = 0.15f + random.nextFloat() * 0.45f; // soft base alpha
            star.twinkleSpeed = 0.8f + random.nextFloat() * 1.5f; // speed of organic breathing
            star.twinklePhase = random.nextFloat() * (float) Math.PI * 2f;
            star.color = starColors[random.nextInt(starColors.length)];
            stars.add(star);
        }

        // Generate 3 slowly floating Nebula Clouds
        nebulas.clear();
        
        // Nebula 1: Soft Green
        Nebula neb1 = new Nebula();
        neb1.x = w * 0.25f;
        neb1.y = h * 0.3f;
        neb1.radius = Math.min(w, h) * 0.65f;
        neb1.vx = 4 + random.nextFloat() * 8; // extremely slow drift in pixels/sec
        neb1.vy = -3 - random.nextFloat() * 6;
        neb1.color = ContextCompat.getColor(getContext(), R.color.pastel_green_mist);
        neb1.maxAlpha = 0.45f;
        nebulas.add(neb1);

        // Nebula 2: Soft Orange/Gold
        Nebula neb2 = new Nebula();
        neb2.x = w * 0.75f;
        neb2.y = h * 0.65f;
        neb2.radius = Math.min(w, h) * 0.75f;
        neb2.vx = -5 - random.nextFloat() * 7;
        neb2.vy = 4 + random.nextFloat() * 6;
        neb2.color = ContextCompat.getColor(getContext(), R.color.pastel_orange_mist);
        neb2.maxAlpha = 0.40f;
        nebulas.add(neb2);

        // Nebula 3: Soft Teal
        Nebula neb3 = new Nebula();
        neb3.x = w * 0.5f;
        neb3.y = h * 0.85f;
        neb3.radius = Math.min(w, h) * 0.55f;
        neb3.vx = 6 + random.nextFloat() * 9;
        neb3.vy = 5 + random.nextFloat() * 8;
        neb3.color = ContextCompat.getColor(getContext(), R.color.pastel_teal_mist);
        neb3.maxAlpha = 0.35f;
        nebulas.add(neb3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Calculate delta time
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000f;
        lastTime = currentTime;

        // Cap delta time to prevent jumps when waking up
        if (deltaTime > 0.05f) deltaTime = 0.05f;
        
        globalTime += deltaTime;

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 1. Draw gradient background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // 2. Draw and update slow-drifting Nebula Clouds (Auroras)
        for (Nebula nebula : nebulas) {
            // Update position
            nebula.x += nebula.vx * deltaTime;
            nebula.y += nebula.vy * deltaTime;

            // Bounce off boundaries with margin so they stay visible
            float margin = nebula.radius * 0.25f;
            if (nebula.x < -margin) {
                nebula.x = -margin;
                nebula.vx = -nebula.vx;
            } else if (nebula.x > width + margin) {
                nebula.x = width + margin;
                nebula.vx = -nebula.vx;
            }

            if (nebula.y < -margin) {
                nebula.y = -margin;
                nebula.vy = -nebula.vy;
            } else if (nebula.y > height + margin) {
                nebula.y = height + margin;
                nebula.vy = -nebula.vy;
            }

            // Create radial gradient for a soft cloud shape
            int baseColor = nebula.color;
            int alphaInt = (int) (nebula.maxAlpha * 255);
            int colorStart = (baseColor & 0x00FFFFFF) | (alphaInt << 24);
            int colorEnd = (baseColor & 0x00FFFFFF) | (0x00 << 24); // Fade to fully transparent

            RadialGradient nebulaShader = new RadialGradient(
                    nebula.x, nebula.y, nebula.radius,
                    new int[]{colorStart, colorEnd},
                    new float[]{0.0f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            nebulaPaint.setShader(nebulaShader);
            canvas.drawCircle(nebula.x, nebula.y, nebula.radius, nebulaPaint);
        }

        // 3. Draw and update twinkling starfield
        for (Star star : stars) {
            // Update y position (move slowly upwards)
            star.y -= star.speed * deltaTime;
            if (star.y < -star.radius * 3) {
                // Reset to bottom
                star.y = height + star.radius * 3;
                star.x = random.nextFloat() * width;
            }

            // Calculate organic breathing twinkling alpha using sine wave
            float sinVal = (float) Math.sin(globalTime * star.twinkleSpeed + star.twinklePhase);
            // Twinkle ranges from 30% of baseAlpha to 100% of baseAlpha
            float currentAlpha = star.baseAlpha * (0.65f + 0.35f * sinVal);

            // Apply alpha to star color
            int alphaInt = (int) (currentAlpha * 255);
            int finalColor = (star.color & 0x00FFFFFF) | (alphaInt << 24);
            
            // Draw soft outer glow (larger circle, very low alpha)
            int glowAlpha = (int) (currentAlpha * 0.3f * 255);
            int finalGlowColor = (star.color & 0x00FFFFFF) | (glowAlpha << 24);
            starGlowPaint.setColor(finalGlowColor);
            canvas.drawCircle(star.x, star.y, star.radius * 2.8f, starGlowPaint);

            // Draw solid inner core
            starPaint.setColor(finalColor);
            canvas.drawCircle(star.x, star.y, star.radius, starPaint);
        }

        // 4. Request next frame for liquid smooth 60+ FPS animation
        postInvalidateOnAnimation();
    }
}
