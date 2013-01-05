/*
 * Copyright (c) 2013 Michael Zucchi
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

import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVSampleFormat;
import au.notzed.jjmpeg.mediaplayer.MediaPlayer.MediaState;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author notzed
 */
public class JFXMediaPlayer extends Application implements MediaSink, MediaPlayer.MediaListener {

	MediaReader reader;
	JFXVideoRenderer vout;
	JavaAudioRenderer aout;
	Stage primaryStage;
	int index = 0;
	String[] paths;

	@Override
	public void start(Stage primaryStage) {
		// Delay main view until we're ready and sized
		this.primaryStage = primaryStage;

		vout = new JFXVideoRenderer();
		aout = new JavaAudioRenderer();

		reader = new MediaReader();
		reader.setListener(this);

		paths = getParameters().getUnnamed().toArray(new String[0]);
		//paths = new String[]{ "/home/notzed/Videos/big-buck-bunny_trailer.webm.mp4" };

		if (paths.length == 0) {
			System.err.println("No parameters supplied");
			Platform.exit();
		}

		index = 0;
		if (index < paths.length)
			reader.open(paths[index++]);
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public AudioFrame getAudioFrame() throws InterruptedException {
		return aout.getFrame();
	}

	@Override
	public VideoFrame getVideoFrame() throws InterruptedException {
		return vout.getFrame();
	}

	@Override
	public void postSeek(long stampms) {
	}

	@Override
	public void postPlay() {
	}

	@Override
	public void postPause() {
	}

	@Override
	public void postUnpause() {
	}

	@Override
	public void postFinished() {
		vout.stop();
		System.out.println("finished file " + paths[index - 1]);
		if (index < paths.length) {
			System.out.println("loading file " + paths[index]);
			reader.open(paths[index++]);
		}
	}

	@Override
	public void mediaMoved(MediaPlayer player, long newpos) {
	}

	@Override
	public void mediaError(MediaPlayer player, Exception ex) {
		System.out.println("Media error");
		ex.printStackTrace(System.out);
	}
	VideoDecoder vd;
	AudioDecoder ad;
	AVRational videoTB;
	long videoStart;
	//
	int audioSampleRate;
//
	boolean haveVideo;
	boolean haveAudio;

	public void initRenderers() {
		for (MediaDecoder md : reader.streamMap.values()) {
			if (md instanceof VideoDecoder) {
				vd = (VideoDecoder) md;
				videoTB = vd.stream.getTimeBase();
				videoStart = vd.stream.getStartTime();

				vout.setVideoFormat(vd.format, vd.width, vd.height);
				haveVideo = true;
			} else if (md instanceof AudioDecoder) {
				ad = (AudioDecoder) md;

				// Force stereo output for multi-channel data
				int cc = Math.min(2, ad.cc.getChannels());

				ad.setOutputFormat(3, cc, AVSampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				//	aRenderer.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				haveAudio = true;
			}
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		reader.cancel();
		vout.stop();
		//aout.stop();
	}

	void initMedia() {
		System.out.println("Init media");
		//busy.setVisibility(ProgressBar.INVISIBLE);
		try {
			reader.createDefaultDecoders(this);
			initRenderers();

			int width = 320, height = 256;

			if (haveVideo) {
				width = vd.width;
				height = vd.height;
			}

			// Now show GUI
			StackPane root = new StackPane();
			root.getChildren().add(vout);

			Scene scene = new Scene(root, width, height);

			primaryStage.setTitle("JJPlayer: " + reader.getPath());
			primaryStage.setScene(scene);
			primaryStage.show();

			if (haveVideo)
				vout.start();
			if (haveAudio)
				aout.start();

			reader.play();

			//	seek.setMax((int) reader.getDuration());

			//	if (haveAudio)
			//		aRenderer.play();
			//	if (!haveVideo)
			//		view.stop();
			//	seek.postDelayed(updatePosition, 100);
			//	isStarted = true;
			//	hideUI();
		} catch (IOException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void mediaState(MediaPlayer player, MediaState newstate) {
		System.out.println("Media state: " + newstate);
		switch (newstate) {
			case Init:
				// Init the player manually
				Platform.runLater(new Runnable() {
					@Override
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
