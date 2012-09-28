package be.ppareit.hidebar_demo.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import be.ppareit.hidebar_demo.Constants;
import be.ppareit.hidebar_demo.Device;
import be.ppareit.hidebar_demo.R;
import be.ppareit.hidebar_demo.ToggleSystembarReceiver;

public class HideBarWidget extends AppWidgetProvider {

    static final String TAG = HideBarWidget.class.getSimpleName();

    private static Intent sToggleIntent = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive action: " + intent.getAction());

        if (intent.getAction().equals(Constants.ACTION_BARHIDDEN)) {
            context.startService(new Intent(context, UpdateService.class));
        } else if (intent.getAction().equals(Constants.ACTION_BARSHOWN)) {
            context.startService(new Intent(context, UpdateService.class));
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        Log.v(TAG, "onEnabled called");
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Log.v(TAG, "onUpdate called");
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {

            Log.v(TAG, "UpdateService onStart called");
            // Build the widget update
            RemoteViews views = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName widget = new ComponentName(this, HideBarWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(widget, views);
        }

        /**
         * Build the view that shows the correct button and sets an correct on click
         */
        public RemoteViews buildUpdate(Context context) {

            Log.v(TAG, "UpdateService buildUpdate called");

            Device.initialize(context.getApplicationContext());
            sToggleIntent = new Intent(context, ToggleSystembarReceiver.class);

            // Create an Intent to launch ExampleActivity
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                    sToggleIntent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.widget_button_barhidden, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_button_barvisible, pendingIntent);

            // Get the state of the systembar and show the correct image on the button
            Device dev = Device.getInstance();
            if (dev.isSystembarVisible()) {
                views.setViewVisibility(R.id.widget_button_barvisible, View.VISIBLE);
                views.setViewVisibility(R.id.widget_button_barhidden, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_button_barvisible, View.GONE);
                views.setViewVisibility(R.id.widget_button_barhidden, View.VISIBLE);
            }

            return views;

        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }

}
