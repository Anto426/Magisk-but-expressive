package com.topjohnwu.magisk.ui.superuser

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.core.model.su.SuLog
import com.topjohnwu.magisk.core.repository.LogRepository
import com.topjohnwu.magisk.core.su.SuEvents
import com.topjohnwu.magisk.ui.MATCH_UNINSTALLED_PACKAGES_COMPAT
import com.topjohnwu.magisk.ui.RefreshOnResume
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.animation.rememberExpandableCardMotion
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogConfirmButton
import com.topjohnwu.magisk.ui.component.MagiskDialogDismissButton
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserScreen(
    onOpenLogs: () -> Unit = {},
    viewModel: SuperuserComposeViewModel = viewModel(factory = SuperuserComposeViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? UIActivity<*>
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRevoke by remember { mutableStateOf<PolicyUiItem?>(null) }

    RefreshOnResume { viewModel.reload() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeCap = StrokeCap.Round)
                }
            }

            !Info.showSuperUser -> {
                DisabledState()
            }

            else -> {
                PolicyList(
                    items = state.items,
                    onOpenLogs = onOpenLogs,
                    onToggleExpanded = viewModel::toggleExpanded,
                    onToggleEnabled = { item, enabled ->
                        ensureAuthThen(activity, scope) {
                            viewModel.setPolicy(
                                item.uid,
                                if (enabled) SuPolicy.ALLOW else SuPolicy.DENY
                            )
                        }
                    },
                    onSliderFinished = { item, value ->
                        ensureAuthThen(activity, scope) {
                            viewModel.setPolicy(item.uid, sliderValueToPolicy(value))
                        }
                    },
                    onToggleNotify = { item ->
                        ensureAuthThen(activity, scope) { viewModel.toggleNotify(item) }
                    },
                    onToggleLog = { item ->
                        ensureAuthThen(activity, scope) { viewModel.toggleLog(item) }
                    },
                    onRevoke = { item ->
                        if (Config.suAuth) {
                            ensureAuthThen(activity, scope) { viewModel.revoke(item) }
                        } else {
                            pendingRevoke = item
                        }
                    }
                )
            }
        }

        MagiskSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MagiskUiDefaults.SnackbarBottomPadding)
        )
    }

    pendingRevoke?.let { item ->
        MagiskDialog(
            onDismissRequest = { pendingRevoke = null },
            title = stringResource(id = CoreR.string.su_revoke_title),
            icon = Icons.Rounded.Warning,
            iconTint = MaterialTheme.colorScheme.error,
            text = {
                Text(
                    text = stringResource(id = CoreR.string.su_revoke_msg, item.title),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                MagiskDialogConfirmButton(
                    onClick = {
                        pendingRevoke = null
                        scope.launch { viewModel.revoke(item) }
                    },
                    destructive = true
                )
            },
            dismissButton = {
                MagiskDialogDismissButton(onClick = { pendingRevoke = null })
            }
        )
    }
}

private fun ensureAuthThen(
    activity: UIActivity<*>?,
    scope: CoroutineScope,
    block: suspend () -> Unit
) {
    if (!Config.suAuth) {
        scope.launch { block() }
        return
    }
    activity?.withAuthentication { ok ->
        if (ok) scope.launch { block() }
    }
}

@Composable
private fun EmptyStateContent() {
    MagiskEmptyState(
        icon = Icons.Rounded.Security,
        title = stringResource(id = CoreR.string.superuser_policy_none)
    )
}

@Composable
private fun DisabledState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = CoreR.string.unsupport_nonroot_stub_msg),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PolicyList(
    items: List<PolicyUiItem>,
    onOpenLogs: () -> Unit,
    onToggleExpanded: (Int, String) -> Unit,
    onToggleEnabled: (PolicyUiItem, Boolean) -> Unit,
    onSliderFinished: (PolicyUiItem, Float) -> Unit,
    onToggleNotify: (PolicyUiItem) -> Unit,
    onToggleLog: (PolicyUiItem) -> Unit,
    onRevoke: (PolicyUiItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = MagiskUiDefaults.screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.ListItemSpacing)
    ) {
        item {
            SuperuserLogsButton(onClick = onOpenLogs)
        }

        if (items.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .fillParentMaxHeight(0.7f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateContent()
                }
            }
        } else {
            items(items, key = { "${it.uid}:${it.packageName}" }) { item ->
                StylishMagiskPolicyCard(
                    item = item,
                    onToggleExpanded = { onToggleExpanded(item.uid, item.packageName) },
                    onToggleEnabled = { onToggleEnabled(item, it) },
                    onSliderFinished = { onSliderFinished(item, it) },
                    onToggleNotify = { onToggleNotify(item) },
                    onToggleLog = { onToggleLog(item) },
                    onRevoke = { onRevoke(item) }
                )
            }
        }
    }
}

@Composable
private fun SuperuserLogsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(MagiskUiDefaults.PrimaryActionHeight),
        shape = MagiskUiDefaults.MediumShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.HistoryEdu,
                null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(id = CoreR.string.superuser_logs).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StylishMagiskPolicyCard(
    item: PolicyUiItem,
    onToggleExpanded: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSliderFinished: (Float) -> Unit,
    onToggleNotify: () -> Unit,
    onToggleLog: () -> Unit,
    onRevoke: () -> Unit
) {
    val cardMotion = rememberExpandableCardMotion(
        expanded = item.expanded,
        expandedElevation = MagiskUiDefaults.ExpandedCardElevation,
        expandedScale = 1f,
        collapsedScale = 1f
    )

    val containerColor by MagiskMotion.animateColor(
        targetValue = if (item.expanded) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "color"
    )

    val isAllowed = item.policy >= SuPolicy.ALLOW
    val iconPainter = remember(item.uid, item.packageName, item.icon) {
        BitmapPainter(item.icon.asImageBitmap())
    }
    val statusColor = when {
        isAllowed -> MaterialTheme.colorScheme.primary
        item.policy == SuPolicy.RESTRICT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                MagiskMotion.cardContentSpring()
            ),
        shape = MagiskUiDefaults.OrganicShapeReversed,
        onClick = onToggleExpanded,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardMotion.elevation)
    ) {
        Box {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_magisk_outline),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .alpha(0.04f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            shadowElevation = 2.dp
                        ) {
                            Image(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    isAllowed -> AppContext.getString(CoreR.string.grant)
                                    item.policy == SuPolicy.RESTRICT -> AppContext.getString(CoreR.string.restrict)
                                    else -> AppContext.getString(CoreR.string.deny)
                                }.uppercase(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (item.showSlider) {
                        var sliderValue by remember(item.uid, item.policy) {
                            mutableStateOf(policyToSliderValue(item.policy))
                        }
                        Slider(
                            modifier = Modifier.width(80.dp),
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 1f..3f,
                            steps = 1,
                            onValueChangeFinished = { onSliderFinished(sliderValue) }
                        )
                    } else {
                        Switch(
                            checked = isAllowed,
                            onCheckedChange = onToggleEnabled,
                            thumbContent = if (isAllowed) {
                                { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(cardMotion.rotation)
                            .padding(start = 12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                AnimatedVisibility(
                    visible = item.expanded,
                    enter = MagiskMotion.simpleExpandEnter(),
                    exit = MagiskMotion.simpleExpandExit()
                ) {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                12.dp,
                                alignment = Alignment.CenterHorizontally
                            )
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FilledTonalIconButton(
                                    onClick = onToggleNotify,
                                    modifier = Modifier.size(MagiskUiDefaults.IconActionSize),
                                    shape = MagiskUiDefaults.SmallShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (item.notification) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        contentColor = if (item.notification) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = stringResource(id = CoreR.string.superuser_toggle_notification),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(id = CoreR.string.superuser_toggle_notification),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FilledTonalIconButton(
                                    onClick = onToggleLog,
                                    modifier = Modifier.size(MagiskUiDefaults.IconActionSize),
                                    shape = MagiskUiDefaults.SmallShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (item.logging) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        contentColor = if (item.logging) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.BugReport,
                                        contentDescription = stringResource(id = CoreR.string.logs),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(id = CoreR.string.logs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    onClick = onRevoke,
                                    modifier = Modifier.size(MagiskUiDefaults.IconActionSize),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteForever,
                                            contentDescription = stringResource(id = CoreR.string.superuser_toggle_revoke),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(id = CoreR.string.superuser_toggle_revoke),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = item.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Logic components remain identical
data class PolicyUiItem(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val icon: Bitmap,
    val isSharedUid: Boolean,
    val policy: Int,
    val remain: Long,
    val notification: Boolean,
    val logging: Boolean,
    val expanded: Boolean = false
) {
    val title: String
        get() = if (isSharedUid) AppContext.getString(
            CoreR.string.shared_uid_label,
            appName
        ) else appName
    val showSlider: Boolean get() = Config.suRestrict || policy == SuPolicy.RESTRICT
}

data class SuperuserUiState(
    val loading: Boolean = true,
    val items: List<PolicyUiItem> = emptyList()
)

class SuperuserComposeViewModel(
    private val policyDao: com.topjohnwu.magisk.core.data.magiskdb.PolicyDao,
    private val logRepo: LogRepository,
    private val pm: PackageManager
) : ViewModel() {
    private val _state = MutableStateFlow(SuperuserUiState())
    val state: StateFlow<SuperuserUiState> = _state
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var reloadJob: Job? = null
    private var lastReloadAt = 0L

    init {
        reload(force = true)
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            SuEvents.policyChanged.debounce(400).collect {
                reload(force = true)
            }
        }
    }

    fun reload(force: Boolean = false) {
        val previousItems = _state.value.items
        val now = SystemClock.elapsedRealtime()
        if (!force && reloadJob?.isActive == true) {
            return
        }
        if (!force && now - lastReloadAt < MIN_RELOAD_INTERVAL_MS) {
            return
        }
        lastReloadAt = now
        reloadJob?.cancel()
        if (previousItems.isEmpty()) {
            _state.update { it.copy(loading = true) }
        }
        val previousByKey = previousItems.associateBy { policyKey(it.uid, it.packageName) }
        reloadJob = viewModelScope.launch {
            val policies = loadPolicies(previousByKey)
            _state.update { it.copy(loading = false, items = policies) }
        }
    }

    private suspend fun loadPolicies(previousByKey: Map<String, PolicyUiItem>): List<PolicyUiItem> =
        withContext(Dispatchers.IO) {
            if (!Info.showSuperUser) return@withContext emptyList()
            policyDao.deleteOutdated(); policyDao.delete(AppContext.applicationInfo.uid)
            val previousByUid = previousByKey.values.groupBy { it.uid }
            val latestLogsByUid = logRepo.fetchSuLogs()
                .asSequence()
                .distinctBy { it.fromUid }
                .associateBy { it.fromUid }
            val policies = mutableListOf<PolicyUiItem>()
            for (policy in policyDao.fetchAll()) {
                val sizeBeforePolicy = policies.size
                val pkgs =
                    if (policy.uid == Process.SYSTEM_UID) arrayOf("android") else pm.getPackagesForUid(
                        policy.uid
                    )
                if (pkgs.isNullOrEmpty()) {
                    val previous = previousByUid[policy.uid]?.firstOrNull()
                    val item = previous?.withPolicy(policy)
                        ?: latestLogsByUid[policy.uid]?.toPolicyItem(policy)
                    item?.let(policies::add)
                    continue
                }
                pkgs.forEach { pkg ->
                    val item = runCatching {
                        val info = pm.getPackageInfo(pkg, MATCH_UNINSTALLED_PACKAGES_COMPAT)
                        val appInfo = info.applicationInfo
                        val key = policyKey(policy.uid, info.packageName)
                        val previous = previousByKey[key]
                        PolicyUiItem(
                            uid = policy.uid,
                            packageName = info.packageName,
                            appName = appInfo?.loadLabel(pm)?.toString() ?: info.packageName,
                            icon = previous?.icon ?: (appInfo?.loadIcon(pm)
                                ?: pm.defaultActivityIcon).toBitmap(),
                            isSharedUid = info.sharedUserId != null,
                            policy = policy.policy,
                            remain = policy.remain,
                            notification = policy.notification,
                            logging = policy.logging,
                            expanded = previous?.expanded ?: false
                        )
                    }.getOrElse {
                        previousByKey[policyKey(policy.uid, pkg)]?.withPolicy(policy)
                            ?: latestLogsByUid[policy.uid]?.takeIf {
                                it.packageName == pkg || it.packageName.substringBefore(":") == pkg
                            }?.toPolicyItem(policy)
                    }
                    item?.let(policies::add)
                }
                if (policies.size == sizeBeforePolicy) {
                    val previous = previousByUid[policy.uid]?.firstOrNull()
                    val item = previous?.withPolicy(policy)
                        ?: latestLogsByUid[policy.uid]?.toPolicyItem(policy)
                    item?.let(policies::add)
                }
            }
            policies.distinctBy { policyKey(it.uid, it.packageName) }
                .sortedWith(compareBy(
                    { it.appName.lowercase(Locale.ROOT) },
                    { it.packageName }
                ))
        }

    fun toggleExpanded(uid: Int, pkg: String) {
        _state.update { st ->
            st.copy(items = st.items.map {
                if (it.uid == uid && it.packageName == pkg) it.copy(
                    expanded = !it.expanded
                ) else it
            })
        }
    }

    fun setPolicy(uid: Int, p: Int) {
        viewModelScope.launch {
            val item = state.value.items.firstOrNull { it.uid == uid } ?: return@launch
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(policy = p))
            }
            _state.update { st -> st.copy(items = st.items.map { if (it.uid == uid) it.copy(policy = p) else it }) }
            _messages.emit(
                AppContext.getString(
                    if (p >= SuPolicy.ALLOW) CoreR.string.su_snack_grant else CoreR.string.su_snack_deny,
                    item.appName
                )
            )
        }
    }

    fun toggleNotify(item: PolicyUiItem) {
        viewModelScope.launch {
            val uid = item.uid
            val nv = !item.notification
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(notification = nv))
            }
            _state.update { st ->
                st.copy(items = st.items.map {
                    if (it.uid == uid) it.copy(
                        notification = nv
                    ) else it
                })
            }
            _messages.emit(
                AppContext.getString(
                    if (nv) CoreR.string.su_snack_notif_on else CoreR.string.su_snack_notif_off,
                    item.appName
                )
            )
        }
    }

    fun toggleLog(item: PolicyUiItem) {
        viewModelScope.launch {
            val uid = item.uid
            val nv = !item.logging
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(logging = nv))
            }
            _state.update { st -> st.copy(items = st.items.map { if (it.uid == uid) it.copy(logging = nv) else it }) }
            _messages.emit(
                AppContext.getString(
                    if (nv) CoreR.string.su_snack_log_on else CoreR.string.su_snack_log_off,
                    item.appName
                )
            )
        }
    }

    fun revoke(item: PolicyUiItem) {
        viewModelScope.launch {
            val uid = item.uid
            withContext(Dispatchers.IO) { policyDao.delete(uid) }
            _state.update { st -> st.copy(items = st.items.filterNot { it.uid == uid }) }
            _messages.emit(AppContext.getString(CoreR.string.su_snack_deny, item.appName))
        }
    }

    private fun policyKey(uid: Int, packageName: String): String = "$uid:$packageName"

    private fun PolicyUiItem.withPolicy(policy: SuPolicy) = copy(
        policy = policy.policy,
        remain = policy.remain,
        notification = policy.notification,
        logging = policy.logging
    )

    private fun PolicyUiItem.toPolicy(
        policy: Int = this.policy,
        notification: Boolean = this.notification,
        logging: Boolean = this.logging
    ) = SuPolicy(
        uid = uid,
        policy = policy,
        remain = remain,
        notification = notification,
        logging = logging
    )

    private fun SuLog.toPolicyItem(policy: SuPolicy): PolicyUiItem {
        val fallbackName = AppContext.getString(CoreR.string.uid_label, policy.uid)
        val resolvedPackage = packageName.takeUnless { it.isBlank() } ?: fallbackName
        val resolvedAppName = appName.takeUnless { it.isBlank() } ?: resolvedPackage
        val iconPackage = resolvedPackage
            .takeUnless { it == fallbackName }
            ?.substringBefore(":")
        val icon = iconPackage
            ?.let { runCatching { pm.getApplicationIcon(it).toBitmap() }.getOrNull() }
            ?: pm.defaultActivityIcon.toBitmap()
        return PolicyUiItem(
            uid = policy.uid,
            packageName = resolvedPackage,
            appName = resolvedAppName,
            icon = icon,
            isSharedUid = resolvedPackage.contains(":") || resolvedPackage == fallbackName,
            policy = policy.policy,
            remain = policy.remain,
            notification = policy.notification,
            logging = policy.logging
        )
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return SuperuserComposeViewModel(
                ServiceLocator.policyDB,
                ServiceLocator.logRepo,
                AppContext.packageManager
            ) as T
        }
    }

    companion object {
        private const val MIN_RELOAD_INTERVAL_MS = 1200L
    }
}

private fun policyToSliderValue(p: Int): Float = when (p) {
    SuPolicy.DENY -> 1f; SuPolicy.RESTRICT -> 2f; SuPolicy.ALLOW -> 3f; else -> 1f
}

private fun sliderValueToPolicy(v: Float): Int = when (v.toInt()) {
    1 -> SuPolicy.DENY; 2 -> SuPolicy.RESTRICT; 3 -> SuPolicy.ALLOW; else -> SuPolicy.DENY
}
