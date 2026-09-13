package com.example.awarechat_android.app

import android.app.Application

class AwareChatApplication : Application() {
    lateinit var dependencies: AppDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        dependencies = AppDependencies.create(this)
    }
}
