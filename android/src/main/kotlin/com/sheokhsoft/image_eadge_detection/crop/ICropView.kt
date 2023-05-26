package com.sheokhsoft.image_eadge_detection.crop

import android.widget.ImageView
import com.sheokhsoft.image_eadge_detection.view.PaperRectangle


/**
 * Created by pengke on 15/09/2017.
 */
class ICropView {
    interface Proxy {
        fun getPaper(): ImageView
        fun getPaperRect(): PaperRectangle
        fun getCroppedPaper(): ImageView
    }
}