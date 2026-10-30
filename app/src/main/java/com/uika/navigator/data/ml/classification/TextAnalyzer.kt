package com.uika.navigator.data.ml.classification

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import com.uika.navigator.data.ml.classification.utils.ImageUtils
import com.uika.navigator.data.model.DetectedObjectResult
import com.uika.navigator.domain.ml.ObjectDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.romainguy.kotlin.math.Float2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Analyzes the frames passed in from the camera and returns any detected text within the requested
 * crop region.
 */
class TextAnalyzer: ObjectDetector {

    private val options = TextRecognizerOptions
        .Builder()
        .build()

    private val detector = TextRecognition.getClient(options)

    override suspend fun analyze(
        mediaImage: Image,
        rotationDegrees: Int,
        imageCropPercentages: Pair<Int, Int>,
        displaySize: Pair<Int, Int>
    ): Result<DetectedObjectResult> {

        var text: Text?
        var cropRect: Rect?
        var croppedBit: Bitmap?

        withContext(Dispatchers.Default) {

            // We requested a setTargetAspectRatio, but it's not guaranteed that's what the camera
            // stack is able to support, so we calculate the actual ratio from the first frame to
            // know how to appropriately crop the image we want to analyze.
            val imageHeight = mediaImage.height
            val imageWidth = mediaImage.width

            val actualAspectRatio = imageWidth / imageHeight

            val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
            cropRect = Rect(0, 0, imageWidth, imageHeight)

            // If the image has a way wider aspect ratio than expected, crop less of the height so we
            // don't end up cropping too much of the image. If the image has a way taller aspect ratio
            // than expected, we don't have to make any changes to our cropping so we don't handle it
            // here.
            var currentCropPercentages = imageCropPercentages
            if (actualAspectRatio > 3) {
                val originalHeightCropPercentage = currentCropPercentages.first
                val originalWidthCropPercentage = currentCropPercentages.second
                currentCropPercentages =
                    Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
            }

            // If the image is rotated by 90 (or 270) degrees, swap height and width when calculating
            // the crop.
            val cropPercentages = currentCropPercentages
            val heightCropPercent = cropPercentages.first
            val widthCropPercent = cropPercentages.second
            val (widthCrop, heightCrop) = when (rotationDegrees) {
                90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
                else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
            }

            cropRect!!.inset(
                (imageWidth * widthCrop / 2).toInt(),
                (imageHeight * heightCrop / 2).toInt()
            )

            val croppedBitmap =
                ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect!!)
            croppedBit = croppedBitmap

            text = detector.process(InputImage.fromBitmap(croppedBitmap, 0)).await()
        }
            return if (text != null) {
                if (text!!.textBlocks.isNotEmpty()) {
                    // Cari textBlock yang cocok dan dapatkan keyword yang cocok
                    var matchedKeyword: String? = null
                    val textBlock = text!!.textBlocks.firstOrNull { textBlock ->
                        val keyword = findMatchedKeyword(textBlock)
                        if (keyword != null) {
                            matchedKeyword = keyword
                            true
                        } else {
                            false
                        }
                    }
                    if (textBlock != null && matchedKeyword != null) {
                        val boundingBox = textBlock.boundingBox
                        if (boundingBox != null) {
                            val croppedRatio = Float2(
                                boundingBox.centerX() / croppedBit!!.width.toFloat(),
                                boundingBox.centerY() / croppedBit!!.height.toFloat()
                            )

                            val x = displaySize.first * croppedRatio.x
                            val y = displaySize.second * croppedRatio.y

                            Result.success(
                                DetectedObjectResult(
                                    // Gunakan keyword standar sebagai label, bukan teks OCR mentah
                                    label = matchedKeyword!!,
                                    centerCoordinate = Float2(x, y)
                                    )
                                )
                        } else {
                            Result.failure(Exception("Cant detect bounding box"))
                        }
                    }
                    else {
                        Result.failure(Exception("No digits found"))
                    }

                } else {
                    Result.failure(Exception("No detected objects"))
                }
            } else {
                Result.failure(Exception("Null text"))
            }

    }

    /**
     * Mencari keyword yang cocok dari teks yang terdeteksi OCR.
     * Mengembalikan keyword standar (FAI, FTS, FIKES, dll) jika cocok, null jika tidak.
     * Urutan keyword penting: keyword yang lebih panjang/spesifik harus diperiksa duluan
     * (misal "RISE CENTER" sebelum "RISE", "PASCASARJANA" sebelum "PASCA").
     */
    private fun findMatchedKeyword(text: Text.TextBlock): String? {
        val targetKeywords = listOf(
            "PASCASARJANA", "RISE CENTER",
            "FAI", "FTS", "FIKES", "FH", "FKIP", "FEB", "RISE",
            "MASJID", "BPPSI", "PASCA"
        )
        val content = text.text.uppercase().trim()
        return targetKeywords.firstOrNull { keyword -> content.contains(keyword) }
    }


}