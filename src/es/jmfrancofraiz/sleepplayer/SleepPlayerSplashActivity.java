package es.jmfrancofraiz.sleepplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import es.jmfrancofraiz.sleepplayer.SleepPlayer.State;

public class SleepPlayerSplashActivity extends Activity {
	
	private String TAG = "SleepPlayerSplashActivity";
	    
    protected void onStart() {
    	Log.d(TAG, "onStart...");
    	super.onStart();

    	State state = SleepPlayer.getInstance().getState();
    	Log.d(TAG, "SleepPlayer state = "+state);
    	
		if (state == State.PLAYING || state == State.PAUSED || state == State.READY) {
			startActivity(new Intent(SleepPlayerSplashActivity.this, SleepPlayerActivity.class));
		}

    };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate...");
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.splash);
    }
    
	  
}