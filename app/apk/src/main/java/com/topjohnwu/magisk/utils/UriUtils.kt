package com.topjohnwu.magisk.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/** Open a web/action URI only through a resolved activity outside this app. */
fun Context.openExternalUri(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    val target = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return
    if (target.activityInfo.packageName == packageName) return
    intent.setClassName(target.activityInfo.packageName, target.activityInfo.name)
    startActivity(intent)
}
