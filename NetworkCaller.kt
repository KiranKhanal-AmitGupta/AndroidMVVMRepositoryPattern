package com.constructivecoders.wowland.base

import kotlinx.coroutines.Deferred
import retrofit2.Response


interface NetworkCaller {

    fun <ResponseEntity : Any> request(deferred: Deferred<Response<ResponseEntity>>,result: (Result<ResponseEntity>) -> Unit)
}