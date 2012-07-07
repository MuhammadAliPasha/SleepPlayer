/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package es.jmfrancofraiz.sleepplayer;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * La reproduccion se realiza mediante un servicio que correo en background.
 * De esta manera nos aseguramos de que el usuario pueda abandonar la activity
 * sin interrumpir la reproduccion.
 * 
 * @author jmffraiz
 *
 */
public class SleepPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener  {

    public static final String TAG = "SleepPlayer";

    //notification
    private NotificationManager mNotificationManager;
    private Notification notification;
    private static final int HELLO_ID = 1;
    private PendingIntent pendingIntent;
    
    //player
    private MediaPlayer mp;
    private String audioStream;    
    private int milliSecs;
    private OnPlayerReadyListener onPlayerReadyListener;
    private OnBufferingUpdateListener onBufferingUpdateListener;
    private OnCompleteListener onCompleteListener;
    private OnPlayerErrorListener onPlayerErrorListener;
    private long fin;
    private long pausa;
    private int startInSeconds;
    private boolean seeking;
    private int posBeforePlay;
    private State state;
    private int currentPosition;    
    private Handler handlerParada = new Handler();
    private Runnable runnableParada = null;
    public enum State {    IDLE, READY, PLAYING, PAUSED, ERROR    }
    
    private Intent intent;
    
    private DatabaseHelper dbHelper;
    private long dbRowId;
    
    //service
    private final IBinder serviceBinder = new LocalBinder();
    
    public class LocalBinder extends Binder {
        SleepPlayerService getService() {
            return SleepPlayerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SleepPlayerService.onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
        
    @Override
    public void onCreate() {
        
        Log.d(TAG,"SleepPlayerService.onCreate");
        super.onCreate();
        
        //estado del player
        state = State.IDLE;

        //notificacion
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.icon;
        CharSequence tickerText = "Sleep Player";
        long when = System.currentTimeMillis();
        notification = new Notification(icon, tickerText, when);        
        Intent notificationIntent = new Intent(this, SleepPlayerActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        
        //base de datos
        dbHelper = new DatabaseHelper(this);
        dbHelper.open();
        
    }

    public void prepare() {

        Log.d(TAG, "SleepPlayerService.prepare: initializing player");

        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnSeekCompleteListener(this);
        mp.setOnErrorListener(this);
        state = State.IDLE;
        currentPosition = 0;
        
        try {
            mp.setDataSource(audioStream);
            mp.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            onPlayerErrorListener.onPlayerError();
        }
    
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "SleepPlayerService.onPrepared");
        state = State.READY;
        milliSecs = mp.getDuration();
        dbRowId = dbHelper.insert(audioStream, new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()), getDuracion());
        onPlayerReadyListener.onPlayerReady();
    }

    public int getTotalHoras() {
        return (int) ((milliSecs / 1000F) / 3600F);
    }
    
    public int getTotalMinutos() {
        return (int) (((milliSecs / 1000F) / 60F) % 60F);
    }
    
    public int getTotalSegundos() {
        return (int) ((milliSecs / 1000F) % 60F);
    }
    
    public int getCurrentPosition() {
        if (state != State.IDLE) currentPosition = mp.getCurrentPosition();
        return currentPosition;
    }
    
    public int getCurrentHoras() {
        return (int) ((getCurrentPosition() / 1000F) / 3600F);
    }
    
    public int getCurrentMinutos() {
        return (int) (((getCurrentPosition() / 1000F) / 60F) % 60F);
    }
    
    public int getCurrentSegundos() {
        return (int) ((getCurrentPosition() / 1000F) % 60F);
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

    public void setOnPlayerErrorListener(OnPlayerErrorListener onPlayerErrorListener) {
        this.onPlayerErrorListener = onPlayerErrorListener;
    }
    
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "SleepPlayerService.onCompletion: initializing player");
        stop();
        onCompleteListener.onComplete();
    }

    public void stop() {
        Log.d(TAG, "SleepPlayerService.stop");
        
        if (mp == null) return;
        
        if (state == State.PLAYING || state == State.PAUSED || state == State.READY) {
            currentPosition = mp.getCurrentPosition();
            mp.stop();
               mp.release();
        }
        
           state = State.IDLE;
           
           //quita el servicio del foreground
           stopForeground(true);
           
        //base de datos
           dbHelper.updatePosicion(dbRowId, getPosicion());

    }

    public State getState() {
        return state;
    }
    
    public void play(int horas, int minutos, int segundos) {
        
        Log.d(TAG, "SleepPlayerService.play");
        
        mp.start();
        state = State.PLAYING;
        
        long ahora = System.currentTimeMillis();
        fin = ahora + horas * 60 * 60 * 1000;
        fin += minutos * 60 * 1000;
        fin += segundos * 1000;
        
        final int secsToFadeOut = 10;
        final int stepLengthInMillis = 500;
        
        runnableParada = new Runnable() {
            public void run() {
                Log.d(TAG, "SleepPlayerService: Running parada");
                if (System.currentTimeMillis() < fin) {
                    float pend = (fin - System.currentTimeMillis()) / 500F;
                    Log.d(TAG, "SleepPlayerService: Pendiente = " + pend);
                    if (pend < (secsToFadeOut * (1000 / stepLengthInMillis))) {
                        float vol = pend / 19F;
                        Log.d(TAG, "SleepPlayerService: Vol = " + vol);
                        if (state == State.PLAYING) mp.setVolume(vol,vol);
                    }
                    handlerParada.postDelayed(this, stepLengthInMillis);
                } else {
                    Log.i(TAG, "SleepPlayerService: Se acabo el tiempo");
                    onCompletion(mp);
                }
            }
            
        };
        
        //reseteo del handler de parada
        handlerParada.removeCallbacks(runnableParada);
        handlerParada.postDelayed(runnableParada,fin-ahora-(secsToFadeOut*1000));
        
        //servicio a foreground
        notification.setLatestEventInfo(getApplicationContext(), "Sleep Player", audioStream, pendingIntent);
        startForeground(HELLO_ID, notification);
        
    }
    
    public void pause() {
        
        Log.d(TAG, "SleepPlayerService.pause");
        
        if (mp!=null && mp.isPlaying()) {

            //player
            mp.pause();
            pausa = System.currentTimeMillis();
            state = State.PAUSED;
            handlerParada.removeCallbacks(runnableParada);

               //quita el servicio del foreground
               stopForeground(true);
               
            //base de datos
               dbHelper.updatePosicion(dbRowId, getPosicion());

        }
        
    }

    public void seekTo(int pos) {
        Log.d(TAG, "SleepPlayerService.seekTo: Seeking to "+pos);
        if (getState() != State.IDLE) { 
            mp.seekTo(pos);
            seeking = true;
        } else {
            currentPosition=pos;
        }
    }

    public String getPosicion() {
           return String.format("%02d:%02d:%02d", getCurrentHoras(), getCurrentMinutos(), getCurrentSegundos());
    }
    
    public String getDuracion() {
        return String.format("%02d:%02d:%02d", getTotalHoras(), getTotalMinutos(), getTotalSegundos());        
    }
    
    public int getProgress() {
        return (int)(((float)getCurrentPosition()/milliSecs)*100);
    }

    public int getPendingHoras() {
        if (state == State.PLAYING) {
            return (int)((fin - System.currentTimeMillis()) / 1000F / 60F / 60F);
        } else if (state == State.PAUSED) {
            return (int)((fin - pausa) / 1000F / 60F / 60F);
        } else {
            if (startInSeconds != 0) 
                return (int) (((milliSecs - startInSeconds*1000F) / 1000F) / 3600F);
            else
                return getTotalHoras();
        }
    }
    
    public int getPendingMinutos() {
        if (state == State.PLAYING) {
            return (int)(((fin - System.currentTimeMillis()) / 1000F / 60F) % 60F);
        } else if (state == State.PAUSED) {
            return (int)(((fin - pausa) / 1000F / 60F) % 60F);
        } else {
            if (startInSeconds != 0) 
                return (int) ((((milliSecs - startInSeconds*1000F) / 1000F) / 60F) % 60F);
            else
                return getTotalMinutos();
        }
        
    }
    
    public int getPendingSegundos() {
        if (state == State.PLAYING) {
            return (int)(((fin - System.currentTimeMillis()) / 1000F) % 60F);
        } else if (state == State.PAUSED) {
            return (int)(((fin - pausa) / 1000F) % 60F);
        } else {
            if (startInSeconds != 0)
                return (int) (((milliSecs - startInSeconds*1000F)/1000F) % 60F);
            else
                return getTotalSegundos();
        }
    }

    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "SleepPlayerService.onSeekComplete: Seeking complete to "+mp.getCurrentPosition());
        seeking = false;
    }

    public void setAudioStream(String audioStream) {
        this.audioStream = audioStream;
    }
    
    public String getAudioStream() {
        return audioStream;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        state = State.ERROR;
        onPlayerErrorListener.onPlayerError();
        return true;
    }

    public void setStartInSeconds(int startInSeconds) {
        this.startInSeconds = startInSeconds;
    }

    public boolean isSeeking() {
        return seeking;
    }

    public void setPosBeforePlay(int posBeforePlay) {
        this.posBeforePlay = posBeforePlay;
    }

    public int getPosBeforePlay() {
        return posBeforePlay;
    }

    public void setDbRowId(long dbRowId) {
        this.dbRowId = dbRowId;
    }

    public long getDbRowId() {
        return dbRowId;
    }    
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "SleepPlayerService.onDestroy");
        if (state == State.PLAYING) stop();
        mNotificationManager.cancel(HELLO_ID);
        dbHelper.close();
        super.onDestroy();
    }

    public int getMilliSecs() {
        return milliSecs;
    }
        

}
