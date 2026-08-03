@file:Suppress("unused", "DEPRECATION", "OVERRIDE_DEPRECATION")

package com.example.eyesbuddy.sensors

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import com.example.eyesbuddy.data.FaceExpression
import com.example.eyesbuddy.data.FaceSenseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.max

/**
 * Front-camera face sensing for mood-aware thought bubbles and expression reactions.
 * This uses the legacy Android camera face-detection API so it fits the current
 * project without extra camera libraries.
 */
class FaceSenseController {

    private val _state = MutableStateFlow(FaceSenseState())
    @Suppress("unused")
    val state: StateFlow<FaceSenseState> = _state
    @Suppress("unused")
    private val stateAnchor: StateFlow<FaceSenseState> = state

    private val previewSurface = SurfaceTexture(42)
    private var camera: Camera? = null
    private var running = false
    private var lastPublished = FaceSenseState()

    fun start() {
        if (running) return
        running = true
        try {
            val cameraId = findFrontCameraId() ?: findAnyCameraId()
            if (cameraId == null) {
                publish(lostState(message = "No front camera available."))
                return
            }

            val opened = Camera.open(cameraId)
            camera = opened
            opened.setFaceDetectionListener(object : Camera.FaceDetectionListener {
                @Suppress("DEPRECATION")
                override fun onFaceDetection(faces: Array<Camera.Face>, camera: Camera) {
                    val next = if (faces.isNotEmpty()) {
                        faceStateFrom(faces[0])
                    } else {
                        lostState(message = "Where’d you go? Come back.")
                    }
                    publishIfChanged(next)
                }
            })

            try {
                opened.setPreviewTexture(previewSurface)
            } catch (_: Exception) {
                publish(lostState(message = "Face sensing is starting up..."))
            }

            try {
                opened.startPreview()
                val parameters = opened.parameters
                if (parameters.maxNumDetectedFaces > 0) {
                    opened.startFaceDetection()
                } else {
                    publish(lostState(message = "Face detection isn’t supported on this device."))
                }
            } catch (_: Exception) {
                publish(lostState(message = "Face sensing is starting up..."))
            }
        } catch (_: Exception) {
            stop()
            publish(lostState(message = "Face sensing unavailable right now."))
        }
    }

    fun stop() {
        running = false
        camera?.let { cam ->
            try { cam.setFaceDetectionListener(null) } catch (_: Exception) {}
            try { cam.stopFaceDetection() } catch (_: Exception) {}
            try { cam.stopPreview() } catch (_: Exception) {}
            try { cam.release() } catch (_: Exception) {}
        }
        camera = null
    }

    private fun publishIfChanged(next: FaceSenseState) {
        val current = state.value
        val changed = next.detected != current.detected ||
            next.expression != current.expression ||
            next.message != current.message
        if (changed) {
            publish(next.copy(eventKey = lastPublished.eventKey + 1L))
        }
    }

    private fun publish(state: FaceSenseState) {
        lastPublished = state
        _state.value = state
    }

    private fun lostState(message: String): FaceSenseState = FaceSenseState(
        detected = false,
        expression = FaceExpression.LOST,
        confidence = 0f,
        message = message
    )

    private fun faceStateFrom(face: Camera.Face): FaceSenseState {
        val rect: Rect = face.rect
        val centerX = rect.centerX() / 1000f
        val centerY = rect.centerY() / 1000f
        val sizeRatio = max(rect.width(), rect.height()) / 1600f
        val score = face.score.coerceIn(0, 100)

        val expression = when {
            score >= 85 && sizeRatio > 0.28f -> FaceExpression.HAPPY
            score >= 70 && abs(centerX) < 0.2f && abs(centerY) < 0.2f -> FaceExpression.CURIOUS
            score < 45 -> FaceExpression.SHY
            abs(centerX) > 0.45f -> FaceExpression.PLAYFUL
            sizeRatio < 0.14f -> FaceExpression.SHY
            else -> FaceExpression.NEUTRAL
        }

        return FaceSenseState(
            detected = true,
            expression = expression,
            confidence = ((score / 100f) + sizeRatio.coerceIn(0f, 1f)) / 2f,
            smileProbability = if (expression == FaceExpression.HAPPY) 0.85f else 0.35f,
            leftEyeOpenProbability = 0.82f,
            rightEyeOpenProbability = 0.82f,
            headYaw = centerX * 36f,
            headRoll = centerY * 24f,
            message = messageFor(expression, score, sizeRatio, centerX, centerY)
        )
    }

    private fun messageFor(
        expression: FaceExpression,
        score: Int,
        sizeRatio: Float,
        centerX: Float,
        centerY: Float
    ): String {
        return when (expression) {
            FaceExpression.HAPPY -> if (score >= 90) {
                "You’re back. I can feel the smile."
            } else {
                "You’re back. That energy helps."
            }
            FaceExpression.CURIOUS -> if (abs(centerX) > abs(centerY)) {
                "That side glance says curiosity."
            } else {
                "Curiosity unlocked."
            }
            FaceExpression.PLAYFUL -> "Playful mode detected."
            FaceExpression.SLEEPY -> "You look sleepy. Rest is valid."
            FaceExpression.SHY -> "A shy visit? I’m glad you’re here."
            FaceExpression.SURPRISED -> "Whoa, surprised mode."
            FaceExpression.NEUTRAL -> when {
                sizeRatio > 0.4f -> "You’re close. I see you clearly."
                abs(centerX) < 0.15f && abs(centerY) < 0.15f -> "You’re centered. Nice and calm."
                else -> "I can see you."
            }
            FaceExpression.LOST -> "Where’d you go? Come back."
            FaceExpression.UNKNOWN -> "Looking for you..."
        }
    }

    private fun findFrontCameraId(): Int? = findCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)

    private fun findAnyCameraId(): Int? = findCameraId(null)

    private fun findCameraId(facing: Int?): Int? {
        val info = Camera.CameraInfo()
        val count = Camera.getNumberOfCameras()
        for (index in 0 until count) {
            Camera.getCameraInfo(index, info)
            if (facing == null || info.facing == facing) return index
        }
        return null
    }
}
