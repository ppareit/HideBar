package be.ppareit.hidebar_intenttest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    static final String ACTION_SHOW_SYSTEMBAR = "be.ppareit.hidebar.ACTION_SHOW_SYSTEMBAR";
    static final String ACTION_HIDE_SYSTEMBAR = "be.ppareit.hidebar.ACTION_HIDE_SYSTEMBAR";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button showBtn = (Button) findViewById(R.id.show_button);
        showBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Sending action: " + ACTION_SHOW_SYSTEMBAR);
                Context context = MainActivity.this;
                context.sendBroadcast(new Intent(ACTION_SHOW_SYSTEMBAR));
            }
        });
        Button hideBtn = (Button) findViewById(R.id.hide_button);
        hideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Sending action: " + ACTION_HIDE_SYSTEMBAR);
                Context context = MainActivity.this;
                context.sendBroadcast(new Intent(ACTION_HIDE_SYSTEMBAR));
            }
        });

    }
}
