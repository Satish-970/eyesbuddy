package com.example.eyesbuddy.data

/**
 * All expressions the eyes can display. EmotionEngine decides which one
 * is active based on sensor + interaction input; Eye.kt decides how each
 * one is drawn (shape, pupil size, eyelid angle, color tint).
 */
enum class Emotion {
    NEUTRAL,
    HAPPY,      // charging, calm
    CURIOUS,    // single tap / new touch
    SLEEPY,     // idle for a while, or low battery
    SURPRISED,  // shake detected
    ANGRY,      // rapid repeated taps
    EXCITED     // charger just connected
}
