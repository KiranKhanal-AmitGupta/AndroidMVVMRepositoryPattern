package com.constructivecoders.wowland.base

import com.google.gson.annotations.SerializedName

open class BaseResponseEntity(
    @SerializedName("message")
    var message: String = "",
    @SerializedName("error_code")
    var errorCode: String? = null
)