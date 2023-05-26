package com.sheokhsoft.image_eadge_detection


import com.sheokhsoft.image_eadge_detection.processor.Corners
import org.opencv.core.Mat

class SourceManager {
    companion object {
        var pic: Mat? = null
        var corners: Corners? = null
    }
}