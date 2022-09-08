package com.hani.btapp.core.com

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by hanif on 2022-07-30.
 */
class CommunicationScopeImpl @Inject constructor() : CommunicationScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}