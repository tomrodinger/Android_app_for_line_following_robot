package com.hani.btapp.core.service

import com.hani.btapp.Logger
import com.hani.btapp.core.PieceOfWork
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * Created by hanif on 2022-07-27.
 */

class GattCommandQueue(
    private val queueName: String
) {

    private val queue = LinkedList<PieceOfWork>()
    private val mutex = Mutex()

    private var busy = false
    private var lastProcessingTime = 0L
    private var periodicCheckJob: Job? = null

    fun add(work: PieceOfWork) {
        GlobalScope.launch {
            mutex.withLock {
                if (queue.add(work)) {
                    processNextInQueue()
                } else {
                    Logger.log("Queue[$queueName]: Could not add to commands queue")
                }
            }
        }
    }

    fun completeCommand() {
        busy = false
        val work = queue.poll()
        work ?: return
        processNextInQueue()
    }

    fun clear() {
        periodicCheckJob?.cancel()
        lastProcessingTime = 0
        busy = false
        queue.clear()
    }

    private fun processNextInQueue() {
        if (busy) {
            return
        }
        if (queue.isEmpty()) {
            periodicCheckJob?.cancel()
            busy = false
            lastProcessingTime = 0
            return
        }
        busy = true
        lastProcessingTime = System.currentTimeMillis()
        queue.peek()?.let { work ->
            GlobalScope.launch {
                Logger.log("Queue executing ${work.name}")
                work.execute()
            }
        }
    }

}