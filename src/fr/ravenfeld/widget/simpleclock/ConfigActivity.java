package fr.ravenfeld.widget.simpleclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Spinner;

public class ConfigActivity extends Activity implements OnClickListener {

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private Spinner mSpinner;
	public static String SHARED_PREFS_NAME = "widget_simple_clock";
	public static String KEY_COLOR = "color";
	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configactivity);
		mSharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, 0);

		addListenerOnSpinnerItemSelection();
		assignAppWidgetId();
		findViewById(R.id.widgetStartButton).setOnClickListener(this);
	}

	/**
	 * Widget configuration activity,always receives appwidget Id appWidget Id =
	 * unique id that identifies your widget analogy : same as setting view id
	 * via @+id/viewname on layout but appwidget id is assigned by the system
	 * itself
	 */
	private void assignAppWidgetId() {
		Bundle extras = getIntent().getExtras();
		if (extras != null)
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	public void addListenerOnSpinnerItemSelection() {
		mSpinner = (Spinner) findViewById(R.id.spinner1);

	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.widgetStartButton) {
			startWidget();
			saveColor();
		}

	}

	private void saveColor() {
		int color = 0;
		String color_item = mSpinner.getSelectedItem().toString();
		SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
		if (color_item.equalsIgnoreCase(getString(R.string.transparent))) {
			color = android.graphics.Color.TRANSPARENT;
		} else if (color_item.equalsIgnoreCase(getString(R.string.red))) {
			color = R.drawable.background_red;
		} else if (color_item.equalsIgnoreCase(getString(R.string.white))) {
			color = R.drawable.background_white;
		} else if (color_item.equalsIgnoreCase(getString(R.string.black))) {
			color = R.drawable.background_black;
		}
		prefEditor.putInt(KEY_COLOR, color);
		prefEditor.commit();
	}

	/**
	 * This method right now displays the widget and starts a Service to fetch
	 * remote data from Server
	 */
	private void startWidget() {

		// // this intent is essential to show the widget
		// // if this intent is not included,you can't show
		// // widget on homescreen
		Intent intent = new Intent();
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(Activity.RESULT_OK, intent);
		//
		// // start your service
		// // to fetch data from web
		// Intent serviceIntent = new Intent(this, RemoteFetchService.class);
		// serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
		// appWidgetId);
		// startService(serviceIntent);

		// finish this activity
		this.finish();

	}

}
