package com.hani.btapp

import android.util.Log

/**
 * Created by hanif on 2022-07-25.
 */

object Logger {
    private const val TAG = "BLEX_LOG"
    fun log(message: String) {
        Log.d(TAG, message)
    }
}