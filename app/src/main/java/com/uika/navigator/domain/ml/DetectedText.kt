package com.uika.navigator.domain.ml

import com.uika.navigator.data.model.DetectedObjectResult
import com.google.ar.core.Frame

data class DetectedText(
    val detectedObjectResult: DetectedObjectResult,
    val frame: Frame
)
