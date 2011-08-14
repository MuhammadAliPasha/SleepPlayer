package es.jmfrancofraiz.sleepplayer;

public abstract interface OnPlayerReadyListener {
	
	public static final boolean RESET = true;
	public static final boolean NO_RESET = false;
	
	public abstract void onPlayerReady();

}
