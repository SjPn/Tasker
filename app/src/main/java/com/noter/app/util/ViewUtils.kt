package com.noter.app.util

import android.view.View
import android.view.ViewGroup

object ViewUtils {
    fun disableSoundEffectsDeep(view: View?) {
        if (view == null) return
        try {
            view.isSoundEffectsEnabled = false
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    disableSoundEffectsDeep(view.getChildAt(i))
                }
            }
        } catch (_: Exception) { }
    }
}


