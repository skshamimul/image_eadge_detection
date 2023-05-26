package com.sheokhsoft.image_eadge_detection.processor

import org.opencv.core.Point
import org.opencv.core.Size

data class Corners(val corners: List<Point?>, val size: Size)