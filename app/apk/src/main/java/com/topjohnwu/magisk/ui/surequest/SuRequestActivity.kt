package com.topjohnwu.magisk.ui.surequest

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.VMFactory
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.base.ActivityExtension
import com.topjohnwu.magisk.core.base.UntrackedActivity
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.su.SuCallbackHandler
import com.topjohnwu.magisk.core.su.SuCallbackHandler.REQUEST
import com.topjohnwu.magisk.core.wrap
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.card.MagiskElevatedPanel
import com.topjohnwu.magisk.ui.theme.MagiskTheme
import com.topjohnwu.magisk.viewmodel.surequest.SuRequestUiState
import com.topjohnwu.magisk.viewmodel.surequest.SuRequestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

class SuRequestActivity : ComponentActivity(), UntrackedActivity {

    private val extension = ActivityExtension(this)
    private val viewModel: SuRequestViewModel by viewModels { VMFactory }

    override fun attachBaseContext(base: Context) {
        val nightMode = if (Config.darkTheme == Config.Value.DARK_THEME_AMOLED) {
            Configuration.UI_MODE_NIGHT_YES
        } else {
            when (Config.darkTheme) {
                -1 -> base.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                0 -> Configuration.UI_MODE_NIGHT_NO
                else -> Configuration.UI_MODE_NIGHT_YES
            }
        }
        val config = Configuration(base.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        super.attachBaseContext(base.createConfigurationContext(config).wrap())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        extension.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        if (Config.suTapjack) {
            window.decorView.rootView.accessibilityDelegate = EmptyAccessibilityDelegate
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this) {
            viewModel.denyPressed()
        }
        handleIntent(intent)

        setContent {
            MagiskTheme {
                SuRequestEffects(
                    activity = this@SuRequestActivity, extension = extension, viewModel = viewModel
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SuRequestScreen(
                    state = state,
                    onTimeoutSelected = viewModel::setSelectedItemPosition,
                    onTimeoutTouched = viewModel::spinnerTouched,
                    onGrant = viewModel::grantPressed,
                    onDeny = viewModel::denyPressed
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        extension.onSaveInstanceState(outState)
    }

    override fun finish() {
        super.finishAndRemoveTask()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) {
            finish()
            return
        }

        val action = intent.getStringExtra("action")
        if (action == REQUEST) {
            viewModel.handleRequest(intent)
        } else {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    SuCallbackHandler.run(this@SuRequestActivity, action, intent.extras)
                }
                finish()
            }
        }
    }

    private object EmptyAccessibilityDelegate : View.AccessibilityDelegate() {
        override fun sendAccessibilityEvent(host: View, eventType: Int) {}
        override fun performAccessibilityAction(host: View, action: Int, args: Bundle?) = true
        override fun sendAccessibilityEventUnchecked(host: View, event: AccessibilityEvent) {}
        override fun dispatchPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) =
            true

        override fun onPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) {}
        override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {}
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {}

        override fun addExtraDataToAccessibilityNodeInfo(
            host: View, info: AccessibilityNodeInfo, extraDataKey: String, arguments: Bundle?
        ) = Unit

        override fun onRequestSendAccessibilityEvent(
            host: ViewGroup, child: View, event: AccessibilityEvent
        ) = false

        override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProvider? = null
    }
}

@Composable
private fun SuRequestEffects(
    activity: SuRequestActivity, extension: ActivityExtension, viewModel: SuRequestViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UiEffect.Finish -> activity.finish()
                UiEffect.RequestAuthentication -> {
                    extension.withAuthentication(viewModel::onAuthenticationResult)
                }

                is UiEffect.Message -> {
                    context.toast(effect.text.resolve(context), Toast.LENGTH_SHORT)
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun SuRequestScreen(
    state: SuRequestUiState,
    onTimeoutSelected: (Int) -> Unit,
    onTimeoutTouched: () -> Unit,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.45f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!state.showUi) {
                MagiskLoadingState(modifier = Modifier.widthIn(max = 280.dp))
            } else {
                SuRequestPanel(
                    state = state,
                    onTimeoutSelected = onTimeoutSelected,
                    onTimeoutTouched = onTimeoutTouched,
                    onGrant = onGrant,
                    onDeny = onDeny
                )
            }
        }
    }
}

@Composable
private fun SuRequestPanel(
    state: SuRequestUiState,
    onTimeoutSelected: (Int) -> Unit,
    onTimeoutTouched: () -> Unit,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current
    val timeoutItems = stringArrayResource(CoreR.array.allow_timeout).toList()
    val denyText = if (state.denyCountdown > 0) {
        "${stringResource(CoreR.string.deny)} (${state.denyCountdown})"
    } else {
        stringResource(CoreR.string.deny)
    }
    val grantTouchFilter: (MotionEvent) -> Boolean = remember(state.useTapjackProtection) {
        { event ->
            val partiallyObscured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            } else {
                false
            }
            val obscured =
                event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0 || partiallyObscured
            if (obscured && event.action == MotionEvent.ACTION_UP) {
                context.toast(CoreR.string.touch_filtered_warning, Toast.LENGTH_SHORT)
            }
            obscured && state.useTapjackProtection
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    MagiskElevatedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_magisk_outline),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-20).dp)
                    .alpha(0.05f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Row with shield icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = CoreR.string.su_request_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Main App Info Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    packageName = state.iconPackageName ?: state.packageName,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Interactive Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Timeout Selector (Custom Dropdown)
                    Box {
                        Surface(
                            onClick = {
                                if (state.grantEnabled) {
                                    onTimeoutTouched()
                                    dropdownExpanded = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MagiskComponentDefaults.ActionHeight),
                            shape = MagiskComponentDefaults.ControlShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            enabled = state.grantEnabled
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = timeoutItems.getOrNull(state.selectedItemPosition)
                                        .orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        MagiskDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            timeoutItems.forEachIndexed { index, item ->
                                MagiskDropdownMenuItem(
                                    text = item,
                                    selected = index == state.selectedItemPosition,
                                    onClick = {
                                        dropdownExpanded = false
                                        onTimeoutSelected(index)
                                    }
                                )
                            }
                        }
                    }

                    // Warning Text Section
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(id = CoreR.string.su_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Buttons Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onDeny,
                        modifier = Modifier
                            .weight(1f)
                            .height(MagiskComponentDefaults.ActionHeight),
                        shape = MagiskComponentDefaults.ControlShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = denyText.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Button(
                        onClick = onGrant,
                        enabled = state.grantEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(MagiskComponentDefaults.ActionHeight)
                            .pointerInteropFilter(onTouchEvent = grantTouchFilter),
                        shape = MagiskComponentDefaults.ControlShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = CoreR.string.grant).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

private fun UiText.resolve(context: Context): CharSequence {
    return when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> context.getString(resId, *args.toTypedArray())
    }
}
