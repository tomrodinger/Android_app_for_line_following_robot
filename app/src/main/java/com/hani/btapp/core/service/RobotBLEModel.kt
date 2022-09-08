package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-31.
 */
enum class RobotBLEModel {
    BL702,

    ;

    companion object {
        fun parse(s: String): RobotBLEModel? {
            if (s.lowercase().contains("bl") && s.lowercase().contains("702")) {
                return BL702
            }

            return null
        }
    }
}