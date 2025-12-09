package com.constructivecoders.wowland.base

import kotlinx.coroutines.*
import retrofit2.Response


class NetworkCallerImpl : NetworkCaller {
    protected val networkJob = Job()
    protected val uiScope = CoroutineScope(Dispatchers.Main + networkJob)

    override fun <ResponseEntity : Any> request(
        deferred: Deferred<Response<ResponseEntity>>,
        result: (Result<ResponseEntity>) -> Unit
    ) {
        uiScope.launch {
            try {
                val response = deferred.await()
                if (response.isSuccessful && response.body() != null) {
                    val responseEntity = response.body() as ResponseEntity
                    result(Result.Success(responseEntity, response.code()))
                } else if (response.errorBody() != null) {
                    result(Result.Error(java.lang.Exception("Server Error")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result(Result.Error(e))
            }
        }

    }

    fun onCleared() {
        networkJob.cancel()
    }
}
