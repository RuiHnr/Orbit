package com.ruirui.orbit.android.app

import android.app.Application
import com.ruirui.orbit.crypto.ensureCryptoProviders

class OrbitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ensureCryptoProviders()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: OrbitApp
            private set
    }
}
