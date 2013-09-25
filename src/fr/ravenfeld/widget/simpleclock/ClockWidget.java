package fr.ravenfeld.widget.simpleclock;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {
	private static final String TAG = ClockWidget.class.getSimpleName();

	// AlarmClock component package depending on the implementation
	// see
	// http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android
	private static final String[][] ALARM_CLOCK_IMPL = { { "Standard Alarm Clock", "com.android.deskclock", "com.android.deskclock.AlarmClock" },
			{ "Nexus Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.AlarmClock" },
			{ "HTC Alarm Clock", "com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl" },
			{ "Moto Blur Alarm Clock", "com.motorola.blur.alarmclock", "com.motorola.blur.alarmclock.AlarmClock" },
			{ "Samsung Galaxy Clock", "com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage" } };
	private static final int ALARM_CLOCK_IMPL_NAME = 0;
	private static final int ALARM_CLOCK_IMPL_PACKAGE = 1;
	private static final int ALARM_CLOCK_IMPL_CLASS = 2;

	private static AlarmManager alarmManager = null;
	private static PendingIntent pendingRefresh = null;
	private static boolean checkedOpenAlarm = false;
	private static PendingIntent pendingOpenAlarmScreen = null;

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		startAutomaticRefresh(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received intent " + intent.getAction());

		if (pendingRefresh != null
				&& (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_USER_PRESENT.equals(intent.getAction()))) {
			forceRefresh();
		}
		super.onReceive(context, intent);
	}

	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "Disable pending refresh");
		if (alarmManager != null)
			alarmManager.cancel(pendingRefresh);

		// Release everything
		alarmManager = null;
		pendingRefresh = null;
		checkedOpenAlarm = false;
		pendingOpenAlarmScreen = null;

		super.onDisabled(context);
	}

	private void startAutomaticRefresh(Context context) {
		if (alarmManager == null) {
			Log.d(TAG, "Initialize alarm manager");
			alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			int update_interval_ms = context.getResources().getInteger(R.integer.update_interval_ms);

			Intent intent = new Intent(context, ClockWidget.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { 1 }); // appWidgetIds
																					// must
																					// have
																					// a
																					// size
																					// >
																					// 1
																					// for
																					// onUpdate
																					// to
																					// be
																					// called
			pendingRefresh = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Set an alarm to run on top of each minute
			long currentTime = new Date().getTime();
			long millisToNextTopMinute = 60000 - (currentTime % 60000) + 500;
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, millisToNextTopMinute, update_interval_ms, pendingRefresh);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Static variables might be reset after an upgrade of the widget
		startAutomaticRefresh(context);

		// Update all widgets at once
		ComponentName me = new ComponentName(context, ClockWidget.class);
		appWidgetManager.updateAppWidget(me, buildUpdate(context, appWidgetIds));
	}

	private RemoteViews buildUpdate(Context context, int[] appWidgetIds) {
		RemoteViews remoteViews;
		if (displayNextAlarm(context)) {
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout_alarm);
			// Display next alarm if any
			displayNextAlarm(context, remoteViews, R.id.txtAlarm);

		} else {
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		}

		// Hour
		int hour;

		if (DateFormat.is24HourFormat(context)) {
			hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		} else {
			hour = Calendar.getInstance().get(Calendar.HOUR);
		}

		// Minute
		int minute = Calendar.getInstance().get(Calendar.MINUTE);
		String minute_string = String.valueOf(minute);
		if (minute / 10 < 1.0f) {
			minute_string = "0" + String.valueOf(minute);
		}
		remoteViews.setTextViewText(R.id.txtHour, String.valueOf(hour) + "h" + minute_string);
		// remoteViews.setImageViewBitmap(R.id.imgMinute,
		// getMinuteBitmap(context, minute));

		// Date
		String dateFormatString;
		if (DateFormat.getDateFormatOrder(context)[0] == DateFormat.DATE) {
			// General format: day month year
			dateFormatString = context.getString(R.string.date_format_all);
		} else {
			// US Format: month day year
			dateFormatString = context.getString(R.string.date_format_us);
		}
		Date now = new Date();
		CharSequence dateText = DateFormat.format(dateFormatString, now);
		remoteViews.setTextViewText(R.id.txtDate, dateText);

		// Open alarm screen on click
		PendingIntent onClickIntent = getOpenAlarmClockIntent(context);
		if (onClickIntent != null) {
			remoteViews.setOnClickPendingIntent(R.id.widgetClock, onClickIntent);
		}
		changeColorBackground(context, remoteViews, R.id.widgetClock);
		return (remoteViews);
	}

	private void changeColorBackground(Context context, RemoteViews remoteViews, int widgetclockID) {
		int resourceID = context.getSharedPreferences(ConfigActivity.SHARED_PREFS_NAME, 0).getInt(ConfigActivity.KEY_COLOR,
				android.graphics.Color.TRANSPARENT);

		remoteViews.setInt(widgetclockID, "setBackgroundResource", resourceID);
	}

	private boolean displayNextAlarm(Context context) {

		String nextAlarmText = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);

		if (nextAlarmText.length() > 0) {
			return true;
		} else {

			return false;
		}
	}

	private void displayNextAlarm(Context context, RemoteViews remoteViews, int textAlarmID) {
		String nextAlarmText = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		remoteViews.setTextViewText(textAlarmID, nextAlarmText);

	}

	private PendingIntent getOpenAlarmClockIntent(Context context) {
		if (pendingOpenAlarmScreen == null && !checkedOpenAlarm) {
			// TODO Add a delayed pending intent to refresh the clock if alarms
			// are changed
			checkedOpenAlarm = true;
			PackageManager packageManager = context.getPackageManager();
			Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

			ComponentName cn = null;
			for (String[] alarmClockImpl : ALARM_CLOCK_IMPL) {
				try {
					cn = new ComponentName(alarmClockImpl[ALARM_CLOCK_IMPL_PACKAGE], alarmClockImpl[ALARM_CLOCK_IMPL_CLASS]);
					packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
				} catch (NameNotFoundException e) {
					// Try another implementation
					continue;
				}
				Log.d(TAG, String.format("Using %s implementation", alarmClockImpl[ALARM_CLOCK_IMPL_NAME]));
				break;
			}
			if (cn != null) {
				alarmClockIntent.setComponent(cn);
				pendingOpenAlarmScreen = PendingIntent.getActivity(context, 0, alarmClockIntent, 0);
			} else {
				Log.e(TAG, "Unable to find correct AlarmClock implementation on this device");
			}
		}
		return pendingOpenAlarmScreen;
	}

	private void forceRefresh() {
		try {
			pendingRefresh.send();
		} catch (CanceledException e) {
			Log.w(TAG, "explicit pendingRefresh refused");
		}
	}

}
