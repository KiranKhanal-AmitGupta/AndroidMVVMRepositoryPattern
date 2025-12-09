package com.constructivecoders.wowland.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

open class BaseRepository {

    protected val viewModelJob = Job()
    protected val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun onCleared() {
        viewModelJob.cancel()
    }
}