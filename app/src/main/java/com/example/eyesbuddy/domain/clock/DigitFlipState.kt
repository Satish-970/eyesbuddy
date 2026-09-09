package com.example.eyesbuddy.domain.clock

/** Holds one digit's previous and current values so only changed digits animate. */ class DigitFlipState(initial: Char = '0') {
    var value: Char = initial
        private set

    fun update(next: Char): Boolean {
        if (next == value) return false
        value = next
        return true
    }
}
