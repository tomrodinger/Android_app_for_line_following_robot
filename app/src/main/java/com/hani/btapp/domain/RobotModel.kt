package com.hani.btapp.domain

/**
 * Created by hanif on 2022-08-31.
 */
enum class RobotModel(val value: String) {
    R1("R1"),

    ;

    companion object {
        fun parse(s: String) = values().firstOrNull { it.value.lowercase() == s.lowercase() }
    }
}