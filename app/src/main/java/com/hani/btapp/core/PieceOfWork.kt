package com.hani.btapp.core

/**
 * Created by hanif on 2022-08-08.
 */
interface PieceOfWork {
    val name: String
    fun execute()
}