package es.jmfrancofraiz.sleepplayer;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class SleepPlayer extends Application implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener  {
	
	public enum State {
		IDLE, READY, PLAYING, PAUSED, ERROR
	}

	private static final String TAG = "SleepPlayer";
	private static final int NOTIFICATION = 0;
	
	private MediaPlayer mp;
	private NotificationManager mNM;
	private String audioStream;
	
	private OnPlayerReadyListener onPlayerReadyListener;
	private OnBufferingUpdateListener onBufferingUpdateListener;
	private OnCompleteListener onCompleteListener;
	
	private int mediaFileLengthInMilliseconds;
	
	private int totalSegundos;
	private int totalMinutos;
	private int totalHoras;

	private int pausedPendingSegundos;
	private int pausedPendingMinutos;
	private int pausedPendingHoras;
	
	private long fin;
	private long pausado;
	
	private State state;
	
	private Handler handlerParada = new Handler();
	private Runnable runnableParada = null;
	
	private static SleepPlayer sleepPlayerInstance;
	
	@Override
	public void onCreate() {
		Log.d(TAG,"onCreate...");
		super.onCreate();
		state = State.IDLE;
		sleepPlayerInstance = this;
	}

	public static SleepPlayer getInstance() {
		return sleepPlayerInstance;
	}
	
	public void prepare() {

    	Log.d(TAG, "initializing player");

    	//audioStream = "/sdcard/Music/14cd1ac1191f.mp3";
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnSeekCompleteListener(this);
        state = State.IDLE;
        
        try {
			mp.setDataSource(audioStream);
			mp.prepareAsync();
		} catch (Exception e) {
			Toast.makeText(SleepPlayer.this, getString(R.string.error_retrieving_stream), Toast.LENGTH_SHORT).show();
			state = State.ERROR;
		}
	
    }

	public void onPrepared(MediaPlayer mediaPlayer) {
		
		mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
		totalSegundos = (int) ((mediaFileLengthInMilliseconds / 1000) % 60);
		totalMinutos = (int) (((mediaFileLengthInMilliseconds / 1000) / 60) % 60);
		totalHoras = (int) ((mediaFileLengthInMilliseconds / 1000) / 3600);
		
		state = State.READY;
		onPlayerReadyListener.onPlayerReady();

	}

	public int getTotalHoras() {
		return totalHoras;
	}
	
	public int getTotalMinutos() {
		return totalMinutos;
	}
	
	public int getTotalSegundos() {
		return totalSegundos;
	}
	
	public int getCurrentHoras() {
		return (int) (mp.getCurrentPosition() / 1000) / 3600;
	}
	
	public int getCurrentMinutos() {
		return (int) (((mp.getCurrentPosition() / 1000) / 60) % 60);
	}
	
	public int getCurrentSegundos() {
		return (int) ((mp.getCurrentPosition() / 1000) % 60);
	}	
	
    public void showNotification() {
        Notification notification = new Notification(R.drawable.icon, audioStream, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        PendingIntent contentIntent = PendingIntent.getActivity(SleepPlayer.this, 0, new Intent(SleepPlayer.this, SleepPlayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(SleepPlayer.this, audioStream,audioStream, contentIntent);
        mNM.notify(NOTIFICATION, notification);
        //context.startForeground(NOTIFICATION, notification);
    }
    
    public void cancelNotification() {
		//stopForeground(true);
		mNM.cancelAll();
    }
    
	public void setOnPlayerReadyListener(OnPlayerReadyListener onPlayerReadyListener) {
		this.onPlayerReadyListener=onPlayerReadyListener;
	}

	public void setOnBufferingUpdateListener (OnBufferingUpdateListener onBufferingUpdateListener) {
		this.onBufferingUpdateListener = onBufferingUpdateListener;
	}

	public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
		this.onCompleteListener = onCompleteListener;
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		onBufferingUpdateListener.onBufferingUpdate(mp, percent);
	}

	public void onCompletion(MediaPlayer mp) {
		int pos = mp.getCurrentPosition();
		int dur = mp.getDuration();
		stop();
		onCompleteListener.onComplete(pos,dur); 
	}

	public void stop() {
    	if (state == State.PLAYING || state == State.PAUSED) {
    		mp.stop();
	   	    mp.release();
    	}
   	    state = State.IDLE;
   	    //cancelNotification();
	}

	public State getState() {
		return state;
	}
	
	public void play(int horas, int minutos, int segundos) {
		
		mp.start();
		state = State.PLAYING;
		
		long ahora = System.currentTimeMillis();
		fin = ahora + horas * 60 * 60 * 1000;
		fin += minutos * 60 * 1000;
		fin += segundos * 1000;
		pausado = 0;
		
		runnableParada = new Runnable() {
			public void run() {
				Log.d(TAG, "Running parada");
				if (System.currentTimeMillis() < fin) {
					float pend = (fin - System.currentTimeMillis()) / 500F;
					Log.d(TAG, "Pendiente = " + pend);
					if (pend < 20) { //diez segundos
						float vol = (float)pend / 19F;
						Log.d(TAG, "Vol = " + vol);
						mp.setVolume(vol,vol);
					}
					handlerParada.postDelayed(this, 500);
				} else {
					Log.i(TAG, "Se acabo el tiempo");
					onCompletion(mp);
				}
			}
    		
    	};
		
    	//reseteo del handler de parada
		handlerParada.removeCallbacks(runnableParada);
    	handlerParada.postDelayed(runnableParada,fin-ahora-10000);
		
	}
	
	public void pause() {
		
		if (mp!=null && mp.isPlaying()) {

			pausedPendingHoras = getPendingHoras();
			pausedPendingMinutos = getPendingMinutos();
			pausedPendingSegundos = getPendingSegundos();
			
			mp.pause();
			state = State.PAUSED;
			handlerParada.removeCallbacks(runnableParada);

		}
		
	}

	public int getCurrentPosition() {
		if (mp==null) return 0;
		return mp.getCurrentPosition();
	}

	public int getMediaFileLengthInMilliseconds() {
		return mediaFileLengthInMilliseconds;
	}

	public void seekTo(int pos) {
		if (mp!=null) {
			Log.d(TAG, "Seeking to "+pos);
			mp.seekTo(pos);
		}
	}

	public int getProgress() {
		return (int)(((float)mp.getCurrentPosition()/mediaFileLengthInMilliseconds)*100);
	}

	public int getPendingHoras() {
		if (state == State.PLAYING) {
			return (int)((fin - System.currentTimeMillis() + pausado) / 1000 / 60 / 60);
		} else if (state == State.PAUSED) {
			return pausedPendingHoras;
		} else {
			return totalHoras;
		}
	}
	
	public int getPendingMinutos() {
		if (state == State.PLAYING) {
			return (int)(((fin - System.currentTimeMillis() + pausado) / 1000 / 60) % 60);
		} else if (state == State.PAUSED) {
			return pausedPendingMinutos;
		} else {
			return totalMinutos;
		}
		
	}
	
	public int getPendingSegundos() {
		if (state == State.PLAYING) {
			return (int)(((fin - System.currentTimeMillis() + pausado) / 1000) % 60);
		} else if (state == State.PAUSED) {
			return pausedPendingSegundos;
		} else {
			return totalSegundos;
		}
	}

	public void onSeekComplete(MediaPlayer mp) {
		Log.d(TAG, "Seeking complete to "+mp.getCurrentPosition());
	}

	public void setAudioStream(String audioStream) {
		this.audioStream = audioStream;
	}
	
	public String getAudioStream() {
		return audioStream;
	}

	
}