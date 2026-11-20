package com.amrnsyt.doublelock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, buildViews(context)) }
    }

    private fun buildViews(context: Context): RemoteViews {
        val tap = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetTapReceiver::class.java).setAction(WidgetTapReceiver.ACTION_TAP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteViews(context.packageName, R.layout.widget_blank).apply {
            setOnClickPendingIntent(R.id.widget_root, tap)
        }
    }
}
