package com.minar.birday.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.nextDateFormatted
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@ExperimentalStdlibApi
class EventWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) updateAppWidget(context, appWidgetManager, appWidgetId)
    }
}

@ExperimentalStdlibApi
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val thread = Thread {
        // Get the next events and the proper formatter
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        // Make sure to show if there's more than one event
        var widgetUpcoming = "${formatEventList(nextEvents, true, context, false)} "
        if (nextEvents.isNotEmpty()) widgetUpcoming += "\n ${
            nextDateFormatted(
                nextEvents[0],
                formatter,
                context
            )
        }"

        // Set the texts and the intent
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val views = if (sharedPrefs.getBoolean("dark_widget", false)) RemoteViews(
            context.packageName,
            R.layout.event_widget_dark
        )
        else RemoteViews(context.packageName, R.layout.event_widget_light)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.event_widget_main, pendingIntent)
        views.setTextViewText(R.id.event_widget_text, widgetUpcoming)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    thread.start()
}
