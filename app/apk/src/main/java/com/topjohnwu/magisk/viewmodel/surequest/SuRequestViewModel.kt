package com.topjohnwu.magisk.viewmodel.surequest

import android.content.Intent
import android.content.SharedPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.data.magiskdb.PolicyDao
import com.topjohnwu.magisk.core.ktx.getLabel
import com.topjohnwu.magisk.core.model.su.SuPolicy.Companion.ALLOW
import com.topjohnwu.magisk.core.model.su.SuPolicy.Companion.DENY
import com.topjohnwu.magisk.core.su.SuRequestHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit.SECONDS
import com.topjohnwu.magisk.core.R as CoreR

data class SuRequestUiState(
    val title: String = "",
    val packageName: String = "",
    val iconPackageName: String? = null,
    val selectedItemPosition: Int = 0,
    val grantEnabled: Boolean = false,
    val denyCountdown: Int = 0,
    val showUi: Boolean = false,
    val useTapjackProtection: Boolean = false
)

class SuRequestViewModel(
    policyDB: PolicyDao, private val timeoutPrefs: SharedPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SuRequestUiState())
    val uiState: StateFlow<SuRequestUiState> = _uiState.asStateFlow()

    private val handler = SuRequestHandler(AppContext.packageManager, policyDB)
    private var initialized = false
    private var responded = false
    private var respondAfterAuth = false
    private var timerJob: Job? = null

    fun grantPressed() {
        cancelTimer()
        if (Config.suAuth) {
            respondAfterAuth = true
            sendEffect(UiEffect.RequestAuthentication)
        } else {
            respond(ALLOW)
        }
    }

    fun denyPressed() {
        respond(DENY)
    }

    fun spinnerTouched() {
        cancelTimer()
    }

    fun setSelectedItemPosition(position: Int) {
        _uiState.update { it.copy(selectedItemPosition = position) }
    }

    fun onAuthenticationResult(granted: Boolean) {
        if (respondAfterAuth && granted) {
            respond(ALLOW)
        }
        respondAfterAuth = false
    }

    fun handleRequest(intent: Intent) {
        viewModelScope.launch(Dispatchers.Default) {
            val started = handler.start(intent)
            withContext(Dispatchers.Main) {
                if (started) showDialog() else sendEffect(UiEffect.Finish)
            }
        }
    }

    private fun showDialog() {
        val pm = handler.pm
        val info = handler.pkgInfo
        val app = info.applicationInfo

        val state = if (app == null) {
            val packageName = info.sharedUserId.toString()
            SuRequestUiState(
                title = AppContext.getString(
                    CoreR.string.shared_uid_label, info.sharedUserId.orEmpty()
                ),
                packageName = packageName,
                selectedItemPosition = timeoutPrefs.getInt(packageName, 0),
                useTapjackProtection = Config.suTapjack,
                showUi = true
            )
        } else {
            val label = app.getLabel(pm)
            val title = if (info.sharedUserId == null) {
                label
            } else {
                AppContext.getString(CoreR.string.shared_uid_label, label)
            }
            SuRequestUiState(
                title = title,
                packageName = info.packageName,
                iconPackageName = info.packageName,
                selectedItemPosition = timeoutPrefs.getInt(info.packageName, 0),
                useTapjackProtection = Config.suTapjack,
                showUi = true
            )
        }

        _uiState.value = state
        startTimer()
        initialized = true
    }

    private fun respond(action: Int) {
        if (!initialized || responded) return
        responded = true
        cancelTimer()

        val state = _uiState.value
        timeoutPrefs.edit().putInt(state.packageName, state.selectedItemPosition).apply()

        viewModelScope.launch {
            handler.respond(action, Config.Value.TIMEOUT_LIST[state.selectedItemPosition])
            sendEffect(UiEffect.Finish)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            val totalSeconds = Config.suDefaultTimeout
            for (sec in totalSeconds downTo 1) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            grantEnabled = it.grantEnabled || (totalSeconds - sec) >= 1,
                            denyCountdown = sec
                        )
                    }
                }
                delay(1000)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(denyCountdown = 0) }
                respond(DENY)
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(denyCountdown = 0) }
    }

    override fun onCleared() {
        respond(DENY)
        super.onCleared()
    }
}
