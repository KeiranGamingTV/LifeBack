package com.keiranhaas.getyourlifeback.util

import android.content.Context
import android.content.Intent

fun Context.goHome() {
    startActivity(
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}