/*
 * Copyright (c) 2012 Michael Zucchi
 *
 * This file is part of jjmpeg.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg.mediaplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVSampleFormat;
import au.notzed.jjmpeg.R;
import au.notzed.jjmpeg.mediaplayer.MediaPlayer.MediaState;
import au.notzed.jjmpeg.mediaplayer.MediaPlayer.Whence;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Media player activity, obviously.
 *
 * @author notzed
 */
public class AndroidMediaPlayer extends Activity implements MediaSink, MediaPlayer.MediaListener {

	GLVideoView view;
	FrameLayout frame;
	SeekBar seek;
	ProgressBar busy;
	TextView debugInfo;
	//
	String filename;
	MediaReader reader;
	//
	AndroidAudioRenderer aout;
	GLESVideoRenderer vout;
	//
	AVRational videoTB;
	long videoStart;
	//
	int audioSampleRate;
	//
	VideoDecoder vd;
	AudioDecoder ad;
	//
	AudioTrack track;
	//
	boolean fingerDown;
	boolean updateSeek;
	boolean haveVideo;
	boolean haveAudio;
	boolean showDebug;
	int throttleRate = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		reader = new MediaReader();
		reader.setListener(this);

		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		frame = new FrameLayout(this);
		seek = new SeekBar(this);

		FrameLayout.LayoutParams fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		view = new GLVideoView(this, reader.getMediaClock());
		view.setKeepScreenOn(true);

		frame.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				System.out.println("frame touched");
				userActive();
				return false;
			}
		});

		//frame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		//fl.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

		frame.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
			boolean working;

			public void onSystemUiVisibilityChange(int visibility) {
				// visibility on, show other stuff too
				if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
					userActive();
				}
			}
		});

		frame.addView(view, fp);
		setContentView(frame);

		vout = view.renderer;
		aout = new AndroidAudioRenderer(reader.getMediaClock());

		Intent it = getIntent();
		System.out.println("intent action = " + it.getAction());
		System.out.println("intent datas = " + it.getDataString());
		System.out.println("intent data  = " + it.getData());

		if (it.getData() != null) {
			if (it.getData().getScheme().equals("content"))
				filename = getRealPathFromURI(it.getData());
			else {
				filename = it.getDataString();
				if (filename.startsWith("file://")) {
					Uri uri = Uri.parse(filename);

					filename = uri.getPath();
				}
			}
		} else {
			filename = "/sdcard/trailer.mp4";
		}

		busy = new ProgressBar(this);
		busy.setIndeterminate(true);
		busy.setVisibility(ProgressBar.INVISIBLE);
		fp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		frame.addView(busy, fp);

		fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
		frame.addView(seek, fp);

		fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
		debugInfo = new TextView(this);
		debugInfo.setTypeface(Typeface.MONOSPACE);
		frame.addView(debugInfo);

		seek.setPadding(28, 28, 28, 28);
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (reader != null && !updateSeek) {
					reader.seek(progress, Whence.Start);
					userActive();
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				fingerDown = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				fingerDown = false;
			}
		});

		hideUI();

		readPreferences();
	}
	/**
	 * User did something, keep the ui around a bit longer
	 */
	boolean shown = false;
	int hidekey = 0;

	void userActive() {
		if (!shown) {
			showUI();
		}
		hidekey++;
		frame.postDelayed(new Runnable() {
			int key = hidekey;

			public void run() {
				System.out.println("delayed run key =" + key + " hide " + hidekey);
				if (key == hidekey) {
					hideUI();
				}
			}
		}, 3000);
	}

	void showUI() {
		System.out.println("Showing ui, seek shoudl be visible");
		seek.setVisibility(View.VISIBLE);
		getActionBar().show();
		shown = true;
	}

	void hideUI() {
		System.out.println("Hiding ui, seek shoudl be invisible");
		seek.setVisibility(View.INVISIBLE);
		frame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		getActionBar().hide();
		shown = false;
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
			case R.id.menu_prefs:
				startActivityForResult(new Intent(this, SettingsActivity.class), 0);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case 0:
				readPreferences();
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	void readPreferences() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		showDebug = sp.getBoolean("pref_debug", false);
		throttleRate = Integer.valueOf(sp.getString("pref_throttle", "1"));
		if (vd != null)
			vd.setThrottleRate(throttleRate);
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (reader.getMediaState() == MediaState.Idle) {
			busy.setVisibility(ProgressBar.VISIBLE);

			reader.open(filename);
		}
	}
	/**
	 * Tracks if we've been through the postPause state.
	 */
	boolean isPaused = false;
	boolean isStarted = false;

	@Override
	protected void onPause() {
		super.onPause();

		if (reader != null) {
			reader.pause();
			isPaused = true;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isPaused) {
			isPaused = false;
			if (reader != null) {
				reader.unpause();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Should this close down the threads/etc?
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		System.out.println("Destroy jjplayer");

		if (reader != null)
			reader.cancel();

		aout.release();
		reader = null;
	}
	// Polls the play position and updates the scrollbar
	Runnable updatePosition = new Runnable() {
		public void run() {
			if (reader != null) {
				if (showDebug) {
					debugInfo.setVisibility(View.VISIBLE);
					debugInfo.setText(
							String.format("Texture Load: %s    Sync Load:    %s    Frame Copy:   %s\nDecode Time:  %s    Render Time:  %s    Frames Dropped:  %12d\n"
							+ "Audio Ready:  %12d       Video Ready   %12d",
							vout.load,
							vout.sync,
							vout.copy,
							vout.decode,
							vout.render,
							vout.framesDropped,
							AndroidAudioRenderer.NBUFFERS - aout.buffers.count(),
							vout.ready.count()));
				} else {
					debugInfo.setVisibility(View.INVISIBLE);
				}
				if (!fingerDown) {
					updateSeek = true;
					seek.setProgress((int) vout.getPosition());
					updateSeek = false;
				}
				seek.postDelayed(updatePosition, 1000);
			}
		}
	};

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA,};
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

		if (cursor == null)
			return contentUri.getPath();

		cursor.moveToFirst();

		int path = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		System.out.println("path " + path + ": " + cursor.getString(path));
		String res = cursor.getString(path);

		cursor.close();

		return res;
	}

	void initMedia() {
		busy.setVisibility(ProgressBar.INVISIBLE);
		try {
			if (reader != null) {
				reader.createDefaultDecoders(this);
				initRenderers();

				seek.setMax((int) reader.getDuration());

				if (haveAudio)
					aout.start();
				if (!haveVideo)
					view.stop();
				else {
					vout.setVideoAspect((float) vd.getDisplayAspectRatio());
				}
				seek.postDelayed(updatePosition, 100);
				isStarted = true;
				hideUI();
			}
		} catch (IOException ex) {
			Logger.getLogger(AndroidMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/*void open(String file) throws IOException {
	 reader = new MediaReader(file);
	 reader.createDefaultDecoders(this);

	 initRenderers();

	 seek.setMax((int) reader.getDuration());
	 }*/
	public void initRenderers() {
		vd = null;
		ad = null;
		for (MediaDecoder md : reader.streamMap.values()) {
			if (md instanceof VideoDecoder) {
				vd = (VideoDecoder) md;
				videoTB = vd.stream.getTimeBase();
				videoStart = vd.stream.getStartTime();

				vout.setVideoSize(vd.width, vd.height);
				vd.setThrottleRate(throttleRate);
				haveVideo = true;
			} else if (md instanceof AudioDecoder) {
				ad = (AudioDecoder) md;

				// Force stereo output for multi-channel data
				int cc = Math.min(2, ad.cc.getChannels());

				ad.setOutputFormat(3, cc, AVSampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				aout.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				haveAudio = true;
			}
		}
	}

	public long getDisplayTime() {
		return Math.max(0, clock);
	}
	Integer dummy = Integer.valueOf(0);
	long clock;
	long startms = -1;
	long seekoffset = 0;

	public void postPlay() {
	}

	public void postSeek(long stamp) {
		// TODO: none of this is used yet
		clock = stamp;
		startms = -1;
		seekoffset = stamp;

		//vout.postSeek(stamp);
		//aout.postSeek(stamp);

		runOnUiThread(doUpdateSeek);
	}
	Runnable doUpdateSeek = new Runnable() {
		public void run() {
			System.out.println("post seek gui thread seek to " + seekoffset);
			updateSeek = true;
			seek.setProgress((int) seekoffset);
			updateSeek = false;
		}
	};

	public void postPause() {
		// could do something so it pauses immediately?
		//aout.pause();
		//vout.pause();
	}

	public void postUnpause() {
		// ensure we re-sync delay
		startms = -1;
		//aout.play();
		//vout.play();
	}

	public void postFinished() {
		if (vout != null) {
			vout.stop();
			view.stop();
			aout.stop();
			long start = (vout.threadLast - vout.thread) / 1000;
			System.err.printf(" GL thread finished cpu time = %d.%06ds\n", start / 1000000L, start % 1000000L);
		}
	}

	public VideoFrame getVideoFrame() throws InterruptedException {
		// Update ui
		return vout.getFrame();
	}

	public AudioFrame getAudioFrame() throws InterruptedException {
		return aout.getFrame();
	}

	public long getMediaPosition() {
		if (haveAudio)
			return aout.getPosition();
		else
			return vout.getPosition();
	}

	// TODO: how do i handle this object going away?
	public void mediaMoved(MediaPlayer player, long newpos) {
		if (reader == null)
			return;
		// Use this to update position perhaps
	}

	public void mediaError(MediaPlayer player, final Exception ex) {
		if (reader == null)
			return;

		// This only happens for a fatal error

		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(AndroidMediaPlayer.this, ex.getLocalizedMessage(), 500).show();
				busy.setVisibility(ProgressBar.INVISIBLE);
			}
		});
	}

	public void mediaState(MediaPlayer player, MediaState newstate) {
		System.out.println("Media state changed: " + newstate);
		// We were destroyed
		if (reader == null)
			return;

		switch (newstate) {
			case Init:
				// Init the player manually
				runOnUiThread(new Runnable() {
					public void run() {
						initMedia();
					}
				});
				break;
			case Ready:
				// Just start it playing right away
				player.play();
				break;
		}
	}
}
