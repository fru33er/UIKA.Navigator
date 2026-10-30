package com.uika.navigator.domain.hit_test

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

data class OrientatedPosition(
    val position: Float3,
    val orientation: Quaternion
)