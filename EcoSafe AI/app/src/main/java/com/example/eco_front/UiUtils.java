package com.example.eco_front;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class UiUtils {

    /**
     * Applies a premium, bouncy 3D spring interaction effect to one or more views (simulating modern CSS active/hover scaling).
     * Shrinks scale, depresses elevation/shadow, and slightly fades on touch.
     * Springs back with overshoot and restores shadow on release.
     */
    public static void setupPremiumBouncyCard(View... views) {
        for (View view : views) {
            if (view == null) continue;
            view.setOnTouchListener(new View.OnTouchListener() {
                private float originalTranslationZ = -1f;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Cache the original elevation on first touch
                    if (originalTranslationZ == -1f) {
                        originalTranslationZ = v.getTranslationZ();
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // CSS active state: Scale down, translate slightly down, drop shadow elevation
                            v.animate()
                             .scaleX(0.96f)
                             .scaleY(0.96f)
                             .translationY(4f) // visual sink
                             .translationZ(originalTranslationZ - 6f) // depress shadow depth
                             .alpha(0.88f)
                             .setDuration(120)
                             .setInterpolator(new OvershootInterpolator(0.8f))
                             .start();
                            break;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // CSS hover state release: bounce back smoothly with clean overshoot spring
                            v.animate()
                             .scaleX(1.0f)
                             .scaleY(1.0f)
                             .translationY(0f)
                             .translationZ(originalTranslationZ)
                             .alpha(1.0f)
                             .setDuration(240)
                             .setInterpolator(new OvershootInterpolator(2.0f))
                             .start();
                            break;
                    }
                    return false; // Very important: let the onClick listener fire standard actions!
                }
            });
        }
    }
}
