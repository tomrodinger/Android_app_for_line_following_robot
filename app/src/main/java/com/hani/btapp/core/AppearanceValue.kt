package com.hani.btapp.core

import com.hani.btapp.utils.toInt

/**
 * Created by hanif on 2022-08-04.
 */

// https://specificationrefs.bluetooth.com/assigned-values/Appearance%20Values.pdf
// https://developer.nordicsemi.com/nRF5_SDK/nRF51_SDK_v4.x.x/doc/html/group___b_l_e___a_p_p_e_a_r_a_n_c_e_s.html#ga9e49b6a59be5a7b7d3fc2d6bb7d33c82

data class AppearanceValueRange(val start: Int, val end: Int)

enum class BleAppearance(val range: AppearanceValueRange) {
    PHONE(AppearanceValueRange(start = 0x0040, end = 0x007F)),
    COMPUTER(AppearanceValueRange(start = 0x0080, end = 0x00BF)),
    WATCH(AppearanceValueRange(start = 0x00C0, end = 0x00FF)),
    CLOCK(AppearanceValueRange(start = 0x0100, end = 0x013F)),
    DISPLAY(AppearanceValueRange(start = 0x0140, end = 0x017F)),

    UNKNOWN(AppearanceValueRange(start = -1, end = -1)),

    ;

    companion object {
        fun fromByteArray(byteArray: ByteArray): BleAppearance {
            val intValue = byteArray.toInt()
            return values().firstOrNull {
                intValue >= it.range.start && intValue <= it.range.end
            } ?: UNKNOWN
        }
    }
}