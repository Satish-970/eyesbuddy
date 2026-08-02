package com.example.eyesbuddy.data

/**
 * Emotional states supported by the companion renderer and behavior engine.
 * The engine moves between these every second so the character never settles
 * into a static idle loop.
 */
enum class Emotion {
    HAPPY,
    CURIOUS,
    SLEEPY,
    PLAYFUL,
    THINKING,
    SHY,
    EXCITED,
    RELAXED,
    SURPRISED,
    SCARED,
    EMBARRASSED,
    CHARGING,
    FULL_BATTERY,
    LOW_BATTERY
}
