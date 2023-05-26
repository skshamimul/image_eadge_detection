package com.sheokhsoft.image_eadge_detection.scan

import android.view.Display
import android.view.SurfaceView
import com.sheokhsoft.image_eadge_detection.view.PaperRectangle

interface IScanView {
    interface Proxy {
        fun exit()
        fun getCurrentDisplay(): Display?
        fun getSurfaceView(): SurfaceView
        fun getPaperRect(): PaperRectangle
    }
}