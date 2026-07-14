package com.topjohnwu.magisk.arch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.viewmodel.home.HomeViewModel
import com.topjohnwu.magisk.viewmodel.install.InstallViewModel
import com.topjohnwu.magisk.viewmodel.superuser.SuperuserViewModel
import com.topjohnwu.magisk.viewmodel.surequest.SuRequestViewModel

object VMFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(ServiceLocator.networkService)
            SuperuserViewModel::class.java -> SuperuserViewModel(
                ServiceLocator.policyDB,
                ServiceLocator.logRepo,
                AppContext.packageManager
            )
            InstallViewModel::class.java ->
                InstallViewModel()
            SuRequestViewModel::class.java ->
                SuRequestViewModel(ServiceLocator.policyDB, ServiceLocator.timeoutPrefs)
            else -> modelClass.getDeclaredConstructor().newInstance()
        } as T
    }
}
