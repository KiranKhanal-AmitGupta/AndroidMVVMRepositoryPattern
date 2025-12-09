package com.constructivecoders.wowland.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonSyntaxException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


abstract class BaseViewModel(val repository: BaseRepository? = null) : ViewModel() {
    internal var _toastLiveData = MutableLiveData<Int>()
    var toastLiveData: LiveData<Int> = _toastLiveData

    internal var _toastLiveDataStr = MutableLiveData<String>()
    var toastLiveDataStr: LiveData<String> = _toastLiveDataStr

    protected val _alertMessageLiveData = MutableLiveData<Int>()
    protected val alertMessageLiveData: LiveData<Int> = _alertMessageLiveData

    /**
     * callbacks to [BaseActivity.observeToast]
     */
    protected fun showToast(stringResId: Int) {
        _toastLiveData.value = stringResId

//        _toastLiveDataStr.value = stringResId.toString()
    }

    /**
     * callbacks to [BaseActivity.observeAlertDxialog]
     */
    protected fun showAlertMessage(stringResId: Int) {
        _alertMessageLiveData.postValue(stringResId)
    }

    fun successHandler(httpCode: Int, successFunc: () -> Unit) {
        when (httpCode) {
            200 -> {
                successFunc()
            }
            201 -> {
                successFunc()
            }
            204 -> {
                successFunc()
            }
        }
    }

    fun errorHandler(
        exception: Throwable? = null,
        errorMsg: String? = null,
        httpCode: Int,
        errorHandler: (errorMsg: String) -> Unit
    ) {
        exception?.printStackTrace()
        val errorMessage = when {
            httpCode in listOf(400, 402, 403, 404) -> {
                errorMsg ?: httpMessage[httpCode] ?: "Error"
            }
            // session expired(Invalid Auth Token)
            httpCode == 401 -> {
//                App.instance.logout()
//                logoutMutableLiveData.value = "Logout"
                return
            }
            //            com.amitgupta.wowland.debugTest -> errorMessage = "Error: " + exception?.localizedMessage
            exception is SocketTimeoutException -> ERROR_CONNECTION_TIMEOUT
            exception is JsonSyntaxException -> ERROR_DATA_COULD_NOT_BE_PARSED
            exception is UnknownHostException -> ERROR_CHECK_INTERNET
            exception is ConnectException -> ERROR_CHECK_INTERNET
            else -> {
                ERROR_UNKNOWN
            }
        }
        errorHandler(errorMessage)
    }

    override fun onCleared() {
        super.onCleared()
        repository?.onCleared()
    }
}
