@file:Suppress("unused")

package com.example.eyesbuddy.data

/** Expression categories inferred from the front camera face analyzer. */
@Suppress("unused")
enum class FaceExpression {
    UNKNOWN,
    NEUTRAL,
    HAPPY,
    CURIOUS,
    SLEEPY,
    SURPRISED,
    PLAYFUL,
    SHY,
    LOST
}

@Suppress("unused")
data class FaceSenseState(
    val detected: Boolean = false,
    val expression: FaceExpression = FaceExpression.UNKNOWN,
    val confidence: Float = 0f,
    val smileProbability: Float = 0f,
    val leftEyeOpenProbability: Float = 1f,
    val rightEyeOpenProbability: Float = 1f,
    val headYaw: Float = 0f,
    val headRoll: Float = 0f,
    val message: String = "Looking for you...",
    val eventKey: Long = 0L
)

@Suppress("unused")
private val faceSenseAnchor = FaceSenseState(
    expression = FaceExpression.UNKNOWN,
    message = ""
)

@Suppress("unused")
private val faceExpressionAnchor = arrayOf(
    FaceExpression.UNKNOWN,
    FaceExpression.NEUTRAL,
    FaceExpression.HAPPY,
    FaceExpression.CURIOUS,
    FaceExpression.SLEEPY,
    FaceExpression.SURPRISED,
    FaceExpression.PLAYFUL,
    FaceExpression.SHY,
    FaceExpression.LOST
)

