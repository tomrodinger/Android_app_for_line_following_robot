package com.hani.btapp.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Created by hanif on 2022-07-23.
 */

inline fun <T> Flow<T>.collectWith(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend CoroutineScope.(T) -> Unit): Job {
    return owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minActiveState) {
            collect {
                action(it)
            }
        }
    }
}