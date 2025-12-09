package com.constructivecoders.wowland.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.constructivecoders.wowland.devtools.loggerDebug

abstract class BaseActivity : AppCompatActivity() {
    abstract val layoutResId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        loggerDebug("onCreate")

        super.onCreate(savedInstanceState)
        setContentView(layoutResId)
    }

    override fun onResume() {
        loggerDebug("onResume")
        super.onResume()
    }

    override fun onPause() {
        loggerDebug("onPause")
        super.onPause()
    }

    override fun onDestroy() {
        loggerDebug("onDestroy")
        super.onDestroy()
    }

    fun handleUI(savedInstanceState: Bundle?){}

    /**
     * setups toolbar
     */
    fun setupToolbar() {

    }

    /**
     * TODO add code to handle fragment within the activity
     */
}