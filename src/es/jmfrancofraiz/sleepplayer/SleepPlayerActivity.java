package es.jmfrancofraiz.sleepplayer;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import es.jmfrancofraiz.sleepplayer.SleepPlayerService.State;

public class SleepPlayerActivity extends Activity  implements OnPlayerReadyListener, OnPlayerErrorListener, OnBufferingUpdateListener, OnCompleteListener, OnSeekBarChangeListener, OnClickListener {
	
	public static final String TAG = "SleepPlayer";
	
	private Button buttonPlayPause;
	private SeekBar seekBar;
	private TextView pendiente;
	private TextView duracion;
	private TextView posicion;
	private TextView textUrl;

	private Handler handlerSegundero = new Handler();
	private Runnable runnableSegundero = null;
	
    private WheelView hours = null;
    private WheelView mins = null;
    private WheelView secs = null;
    
    private boolean completedOnce;

	private ProgressDialog pbarDialog;

	private SleepPlayerService mSleepPlayerService;
	private boolean isBound = false;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	mSleepPlayerService = ((SleepPlayerService.LocalBinder)service).getService();
	    	Log.d(TAG, "SleepPlayerActivity.serviceConnection.onServiceConnected: SleepPlayerService connected");
	    	setup();
	    }
	    public void onServiceDisconnected(ComponentName className) {
	    	mSleepPlayerService = null;
	    	Log.d(TAG, "SleepPlayerActivity.serviceConnection.onServiceDisconnected: SleepPlayerService disconnected");
	    }
	};	
	
	private void doBindService() {
    	Log.d(TAG, "SleepPlayerActivity.doBindService: SleepPlayerService binding");
	    bindService(new Intent(SleepPlayerActivity.this, SleepPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	    isBound = true;
	}

	private void doUnbindService() {
	    if (isBound) {
	        // Detach our existing connection.
	    	Log.d(TAG, "SleepPlayerActivity.doUnbindService: SleepPlayerService unbinding");
	        unbindService(serviceConnection);
	        isBound = false;
	    }
	}
	
	public void setup() {
		
		Log.d(TAG, "SleepPlayerActivity.setup: SleePlayer State = "+mSleepPlayerService.getState());
		
		mSleepPlayerService.setOnPlayerReadyListener(this);
		mSleepPlayerService.setOnBufferingUpdateListener(this);
		mSleepPlayerService.setOnCompleteListener(this);	 
		mSleepPlayerService.setOnPlayerErrorListener(this);
		
    	runnableSegundero = new Runnable() {
	        public void run() {
	    		if (mSleepPlayerService.getState() == State.PLAYING) {
	    			seekBar.setProgress(mSleepPlayerService.getProgress());
	    			posicion.setText(String.format("%02d:%02d:%02d", mSleepPlayerService.getCurrentHoras(), mSleepPlayerService.getCurrentMinutos(), mSleepPlayerService.getCurrentSegundos()));
	    			pendiente.setText(String.format("%02d:%02d:%02d", mSleepPlayerService.getPendingHoras(), mSleepPlayerService.getPendingMinutos(), mSleepPlayerService.getPendingSegundos()));
	    		    handlerSegundero.postDelayed(runnableSegundero,1000);
	        	}
			}
	    };	
	    
		if (getIntent().getData() != null &&
				getIntent().getScheme().equals("http") &&
				(mSleepPlayerService.getIntent() == null || 
				(!mSleepPlayerService.getIntent().getDataString().equals(getIntent().getDataString())))) {
		
			String url = getIntent().getData().getScheme()+":"+getIntent().getData().getSchemeSpecificPart();
			String fragment = getIntent().getData().getFragment();
			Log.d(TAG, "SleepPlayerActivity.setup: url: "+url);
			Log.d(TAG, "SleepPlayerActivity.setup: fragment: "+fragment);
			
	        pbarDialog.show();
	        completedOnce = false;

	        mSleepPlayerService.setIntent(getIntent());
	        mSleepPlayerService.stop();
	        mSleepPlayerService.setAudioStream(url);
	        mSleepPlayerService.prepare();
	        
		} else {
			
			findViewById(R.id.hint).setVisibility(View.VISIBLE);
			
		}
	    
		setupScreen();
		
	}
	
    private void setupScreen() {
    	if (mSleepPlayerService == null) return;
    	if (mSleepPlayerService.getState() == State.PLAYING) {
			playingScreen();
		} else if (mSleepPlayerService.getState() == State.PAUSED || mSleepPlayerService.getState() == State.READY) {
			pausedScreen(false);
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	Log.d(TAG, "SleepPlayerActivity.onCreate");
        Log.d(TAG, "SleepPlayerActivity.onCreate: Intent action = "+getIntent().getAction());
        Log.d(TAG, "SleepPlayerActivity.onCreate: Intent categories = "+getIntent().getCategories());
        Log.d(TAG, "SleepPlayerActivity.onCreate: Intent data = "+getIntent().getDataString());
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        findViewById(R.id.layoutBottom).setVisibility(View.GONE);
        
		//rueda de horas
    	hours = (WheelView) findViewById(R.id.hours); 
        NumericWheelAdapter nwah = new NumericWheelAdapter(this, 0, 99, "%02d");
        hours.setViewAdapter(nwah);
        
        //rueda de minutos
        mins = (WheelView) findViewById(R.id.mins);
        NumericWheelAdapter nwam = new NumericWheelAdapter(this, 0, 59, "%02d");
        mins.setViewAdapter(nwam);

        //rueda de segundos
        secs = (WheelView) findViewById(R.id.secs);
        NumericWheelAdapter nwas = new NumericWheelAdapter(this, 0, 59, "%02d");
        secs.setViewAdapter(nwas);
        
		buttonPlayPause = (Button)findViewById(R.id.ButtonPlayPause);
		buttonPlayPause.setOnClickListener(this);
		buttonPlayPause.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.gray_button));
		
		textUrl = (TextView)findViewById(R.id.textViewUrl);
		
		seekBar = (SeekBar)findViewById(R.id.SeekBarTestPlay);	
		seekBar.setMax(99); // It means 100% .0-99
		seekBar.setOnSeekBarChangeListener(this);
		
		duracion = (TextView)findViewById(R.id.duracion);
		posicion = (TextView)findViewById(R.id.posicion);
		pendiente = (TextView)findViewById(R.id.pendiente);
		
        pbarDialog = new ProgressDialog(this);
        pbarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pbarDialog.setMessage(getString(R.string.cargando));

		//servicio
		//startService();
		doBindService();

    }
	 
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "SleepPlayerActivity.onPostCreate");
    	super.onPostCreate(savedInstanceState);
    }
       
    @Override
    protected void onPostResume() {
    	Log.d(TAG, "SleepPlayerActivity.onPostResume");
    	super.onPostResume();
     }
    
    @Override
    protected void onDestroy() {
    	Log.d(TAG, "SleepPlayerActivity.onDestroy");
    	super.onDestroy();
    	doUnbindService();
    }
    
    @Override
    protected void onRestart() {
    	Log.d(TAG, "SleepPlayerActivity.onRestart");
    	super.onRestart();
    }
    
    @Override
    protected void onPause() {
    	Log.d(TAG, "SleepPlayerActivity.onPause");
    	super.onPause();
    	handlerSegundero.removeCallbacks(runnableSegundero);
    }
    
    @Override
    protected void onResume() {
    	Log.d(TAG, "SleepPlayerActivity.onResume");
    	super.onResume();
    	setupScreen();
    }

        
    @Override
    protected void onStart() {
    	Log.d(TAG, "SleepPlayerActivity.onStart");
    	super.onStart();
    }
    
    @Override
    protected void onStop() {
    	Log.d(TAG, "SleepPlayerActivity.onStop");
    	handlerSegundero.removeCallbacks(runnableSegundero);
    	super.onStop();
    }
    
    @Override
    protected void onNewIntent(Intent i) {
    	Log.d(TAG, "SleepPlayerActivity.onNewIntent");
    	Log.d(TAG, "SleepPlayerActivity.onNewIntent: Intent = "+(i.getData()!=null?i.getDataString():"no data"));
    	super.onNewIntent(i);
    }
    
    @Override
    protected void onUserLeaveHint() {
    	Log.d(TAG, "SleepPlayerActivity.onUserLeaveHint");
    	super.onUserLeaveHint();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.d(TAG, "SleepPlayerActivity.onSaveInstanceState");
    	super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.d(TAG, "SleepPlayerActivity.onRestoreInstanceState");
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    private void playingScreen() {
    	
    	Log.d(TAG, "SleepPlayerActivity.playingScreen");
    	
        findViewById(R.id.splashWindow).setVisibility(View.GONE);
        findViewById(R.id.layoutUrl).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutLabelWheels).setVisibility(View.GONE);
        findViewById(R.id.layoutWheels).setVisibility(View.GONE);
        findViewById(R.id.layoutPendiente).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
    	
    	handlerSegundero.post(runnableSegundero);
    	textUrl.setText(mSleepPlayerService.getAudioStream());

		posicion.setText(mSleepPlayerService.getPosicion());    	
    	duracion.setText(mSleepPlayerService.getDuracion());
		seekBar.setProgress(mSleepPlayerService.getProgress());

    	buttonPlayPause.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.red_button));
		buttonPlayPause.setText(getString(R.string.pausa));
		
    }
    
    private void pausedScreen(boolean anim) {
    	
    	Log.d(TAG, "SleepPlayerActivity.pausedScreen");
    	
        findViewById(R.id.splashWindow).setVisibility(View.GONE);
        findViewById(R.id.layoutUrl).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutLabelWheels).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutWheels).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutPendiente).setVisibility(View.GONE);
        findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
    	
    	handlerSegundero.removeCallbacks(runnableSegundero);
    	textUrl.setText(mSleepPlayerService.getAudioStream());
		
		posicion.setText(mSleepPlayerService.getPosicion());    	
    	duracion.setText(mSleepPlayerService.getDuracion());
    	seekBar.setProgress(mSleepPlayerService.getProgress());
    	
    	buttonPlayPause.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.green_button));
		buttonPlayPause.setText(getString(R.string.reproducir));
		buttonPlayPause.setTextColor(Color.WHITE);
		
		hours.setCurrentItem(mSleepPlayerService.getPendingHoras(), anim);
		mins.setCurrentItem(mSleepPlayerService.getPendingMinutos(), anim);
		secs.setCurrentItem(mSleepPlayerService.getPendingSegundos(), anim);

    }
    
    private void buttonPlayPauseClick() {
    	
    	if (mSleepPlayerService.getState() == State.PLAYING) {
    		
    		mSleepPlayerService.pause();
    		pausedScreen(true);
    		
    	} else if (mSleepPlayerService.getState() == State.IDLE) {
    		
    		pbarDialog.show();
    		
			if (getIntent().getData().getFragment() != null) {
				getIntent().setData(
						Uri.parse(getIntent().getDataString().substring(0,
								getIntent().getDataString().indexOf("#"))
								+ "#" + posicion.getText().toString()));
			} else {
				getIntent().setData(Uri.parse(getIntent().getDataString()+"#"+posicion.getText().toString()));
			}
			
			Log.d(TAG, "Uri = "+getIntent().getDataString());
    		
    		mSleepPlayerService.prepare();
        	
    	} else if (mSleepPlayerService.getState() == State.READY || mSleepPlayerService.getState() == State.PAUSED ) {
    		
    		play();
    		
    	} 	
	}
    
    private void play() {
		mSleepPlayerService.setPosBeforePlay(mSleepPlayerService.getCurrentPosition());
    	mSleepPlayerService.play(hours.getCurrentItem(), mins.getCurrentItem(), secs.getCurrentItem());	        	
		new Handler().postDelayed(new Runnable() {
			public void run() {
	        	int diffPos = Math.abs(mSleepPlayerService.getCurrentPosition() - mSleepPlayerService.getPosBeforePlay());	    			
	    		if (diffPos > 1000 && !mSleepPlayerService.isSeeking()) mSleepPlayerService.seekTo(mSleepPlayerService.getPosBeforePlay());
	    		mSleepPlayerService.setPosBeforePlay(mSleepPlayerService.getCurrentPosition());
			}
		},100);
		playingScreen();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
    	case R.id.acerca_de:

    		LayoutInflater factory = LayoutInflater.from(this);
            final View aboutView = factory.inflate(R.layout.about, null);
            
            ImageButton mailTo = (ImageButton) aboutView.findViewById(R.id.mail);
            mailTo.setOnClickListener(new OnClickListener() {				
				public void onClick(View arg0) {
					Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:jmfrancofraiz@gmail.com"));
					intent.putExtra("subject", "SleepPlayer");
					startActivity(intent);
				}
			});
            
            ImageButton twitter = (ImageButton) aboutView.findViewById(R.id.twitter);
            twitter.setOnClickListener(new OnClickListener() {				
				public void onClick(View arg0) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/#!/jmfrancofraiz"));
					startActivity(intent);
				}
			});
            
    		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    		alertDialog.setTitle("Sleep Player v. 2.0");
    		alertDialog.setView(aboutView);
    		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
    		   public void onClick(DialogInterface dialog, int which) {
    		      dialog.dismiss();
    		   }
    		});
    		alertDialog.setIcon(R.drawable.icon);
    		alertDialog.show();
    		
    		return true;
    		
    	case R.id.historial:
    		startActivity(new Intent(SleepPlayerActivity.this, HistoryActivity.class));
    		return true;
    		
    	default:
    		return super.onOptionsItemSelected(item);
    		
    	}
    	
    	
    }

	public void onPlayerReady() {
		
		Log.d(TAG, "SleepPlayerActivity.onPlayerReady");
		
		if (mSleepPlayerService.getMilliSecs() == 0) {
			
			seekBar.setVisibility(View.GONE);
			duracion.setVisibility(View.GONE);
			
			new AlertDialog.Builder(this).setMessage(getString(R.string.no_duracion))
			   .setNeutralButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					})
				.create()
				.show();
		}
		
		if (getIntent().getData().getFragment() != null) {
			String[] frags = getIntent().getData().getFragment().split(":");
			int pos = Integer.parseInt(frags[0])*60*60*1000 + Integer.parseInt(frags[1])*60*1000 + Integer.parseInt(frags[2])*1000;
	        mSleepPlayerService.seekTo(pos);
	        seekBar.setProgress(mSleepPlayerService.getProgress());
	        posicion.setText(getIntent().getData().getFragment());
	        mSleepPlayerService.setStartInSeconds(pos / 1000);
		}
		
		if (!completedOnce) {
			pausedScreen(false);
		} else {
        	play();
		}
		
		pbarDialog.dismiss();
		
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		seekBar.setSecondaryProgress(percent);
	}

	public void onComplete(MediaPlayer mp) {
		Log.d(TAG, "SleepPlayerActivity.onComplete");
		mSleepPlayerService.setStartInSeconds(mSleepPlayerService.getCurrentPosition() / 1000);
		pausedScreen(true);
		completedOnce = true;
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			int playPositionInMilliseconds = (mSleepPlayerService.getMilliSecs() / 100) * seekBar.getProgress();
			mSleepPlayerService.seekTo(playPositionInMilliseconds);
			posicion.setText(String.format("%02d:%02d:%02d", mSleepPlayerService.getCurrentHoras(), mSleepPlayerService.getCurrentMinutos(),mSleepPlayerService.getCurrentSegundos()));
		}
	}

	public void onStartTrackingTouch(SeekBar arg0) {}

	public void onStopTrackingTouch(SeekBar arg0) {}

	public void onClick(View v) {
		buttonPlayPauseClick();
	}

	public void onPlayerError() {
		
		Log.d(TAG, "SleepPlayerActivity.onPlayerError");
		pbarDialog.dismiss();
		
		new AlertDialog.Builder(this).setMessage(getString(R.string.error_retrieving_stream))
		   .setNeutralButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.dismiss();
						finish();
					}
				})
			.create()
			.show();
	}

	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    //Handle the back button
	    if(keyCode == KeyEvent.KEYCODE_BACK && mSleepPlayerService.getState() == State.PLAYING) {
	    	
	        //Ask the user if they want to quit
	        new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.quit)
	        .setMessage(R.string.really_quit)
	        .setPositiveButton(R.string.si,new DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface dialog, int which) {

	                //Stop the activity
	                SleepPlayerActivity.this.finish();    
	            }

	        })
	        .setNegativeButton(R.string.no,null)
	        .show();

	        return true;
	        
	    } else {
	    	
	        return super.onKeyDown(keyCode, event);
	        
	    }

	}
	*/
	
}
