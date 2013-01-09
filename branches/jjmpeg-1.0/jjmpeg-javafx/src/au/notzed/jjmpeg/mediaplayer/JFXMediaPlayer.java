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

import au.notzed.eofx.EOFXView;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.AVSampleFormat;
import au.notzed.jjmpeg.mediaplayer.MediaPlayer.MediaState;
import au.notzed.jjmpeg.mediaplayer.MediaPlayer.Whence;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author notzed
 */
public class JFXMediaPlayer extends Application implements MediaSink, MediaPlayer.MediaListener {

	StackPane root;
	MediaReader reader;
	JFXVideoRenderer vout;
	JavaAudioRenderer aout;
	JFXMediaControls controls;
	Stage primaryStage;
	int index = 0;
	String[] paths;
	Timeline autoUpdate;

	@Override
	public void start(Stage primaryStage) {
		// Delay main view until we're ready and sized
		this.primaryStage = primaryStage;

		vout = new JFXVideoRenderer();
		aout = new JavaAudioRenderer();

		reader = new MediaReader();
		reader.setListener(this);

		paths = getParameters().getUnnamed().toArray(new String[0]);

		if (paths.length == 0 && new File("/home/notzed/Videos/big-buck-bunny_trailer.webm.mp4").canRead())
			paths = new String[]{"/home/notzed/Videos/big-buck-bunny_trailer.webm.mp4"};

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
		if (haveVideo)
			vout.setAudioLocation(aout.getPosition());
		return aout.getFrame();
	}

	@Override
	public VideoFrame getVideoFrame() throws InterruptedException {
		return vout.getFrame();
	}

	@Override
	public void postSeek(long stampms) {
		if (haveVideo)
			vout.postSeek(stampms);
		if (haveAudio)
			aout.postSeek(stampms);
	}

	@Override
	public void postPlay() {
		System.out.println(" post play");
		if (haveAudio)
			aout.start();
		if (haveVideo)
			vout.unpause();
	}

	@Override
	public void postPause() {
		System.out.println(" post pause");
		if (haveVideo)
			vout.pause();
		if (haveAudio)
			aout.stop();
	}

	@Override
	public void postUnpause() {
		System.out.println(" post unpause");
		if (haveVideo)
			vout.unpause();
		if (haveAudio)
			aout.start();
	}

	@Override
	public void postFinished() {
		vout.stop();
		aout.stop();
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

				// Force stereo output for multi-channel data, for now
				int cc = Math.min(2, ad.cc.getChannels());

				ad.setOutputFormat(3, cc, AVSampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				aout.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				haveAudio = true;
			}
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		reader.cancel();
		vout.release();
		aout.release();
	}

	void initMedia() {
		System.out.println("Init media");
		//busy.setVisibility(ProgressBar.INVISIBLE);
		try {
			reader.createDefaultDecoders(this);
			initRenderers();

			int width = 512, height = 256;
			double aspect = 1.0;

			if (controls != null) {
				controls.setPlayer(null);
				autoUpdate.stop();
			}

			if (haveVideo) {
				width = vd.getWidth();
				height = vd.getHeight();
				aspect = vd.getDisplayAspectRatio();
			}

			System.out.println("aspect ratio: " + aspect);
			vout.setScaleX(aspect);

			// Now show GUI
			root = new StackPane();
			root.getChildren().add(vout);

			root.setId("player-root");

			vout.setPreserveRatio(true);
			// Divide by aspect to compensate for scale
			vout.fitWidthProperty().bind(root.widthProperty().divide(aspect));
			vout.fitHeightProperty().bind(root.heightProperty());

			root.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent t) {
					userActive();
				}
			});

			BorderPane overlay = new BorderPane();
			overlay.setFocusTraversable(true);
			overlay.setMouseTransparent(true);
			// TODO: put the 'actions' somewhere central, perhaps in reader or here
			// remove from controls.
			overlay.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
				boolean wasfull = false;
				long skipMS = 30_000;

				@Override
				public void handle(KeyEvent t) {
					System.out.println("key pressed" + t.getCode());
					switch (t.getCode()) {
						case F11:
							wasfull = true;
							primaryStage.setFullScreen(true);
							break;
						case ESCAPE:
							// For some reason, escape turns off full-screen mode,
							// however, it doesn't capture the event ...
							if (!wasfull)
								Platform.exit();
							wasfull = false;
							break;
						case RIGHT:
							System.out.println("forward skip");
							reader.seek(skipMS, Whence.Here);
							break;
						case LEFT:
							System.out.println("backward skip");
							reader.seek(-skipMS, Whence.Here);
							break;
						case PAGE_UP:
							reader.seek(0, Whence.Start);
							break;
						case PAGE_DOWN:
							reader.seek(reader.getDuration(), Whence.Start);
							break;
						case SPACE:
							System.out.println("toggle pause");
							controls.togglePause();
							break;
						case PRINTSCREEN:
							System.out.println("print screen capture");
							if (haveVideo) {
								WritableImage img = vout.getCurrentFrame();
								if (img != null)
									EOFXView.showImage(img, reader.getPath() + " " + controls.msToString(getMediaPosition()));
							}
						default:
							return;
					}
					t.consume();
				}
			});


			controls = new JFXMediaControls();
			//controls.setPrefWidth(width);
			Group g = new Group(controls);
			StackPane.setAlignment(g, Pos.BOTTOM_CENTER);
			root.getChildren().add(g);
			root.getChildren().add(overlay);

			controls.setPlayer(this);
			controls.setId("player-controls");

			controls.setDuration(reader.getDuration());

			Scene scene = new Scene(root, width * aspect, height);

			scene.getStylesheets().add("/au/notzed/jjmpeg/mediaplayer/style.css");

			primaryStage.setTitle("JJPlayer: " + reader.getPath());
			primaryStage.setScene(scene);
			primaryStage.show();

			if (haveVideo)
				vout.start();
			if (haveAudio) {
				aout.start();
				// Init line position
				aout.postSeek(0);
			}

			//reader.play();
			reader.ready();

			autoUpdate = new Timeline(new KeyFrame(Duration.millis(500), updateHandler));
			autoUpdate.setCycleCount(Timeline.INDEFINITE);
			autoUpdate.play();

			// Reset mouse state
			root.setCursor(Cursor.NONE);
			mouseHidden = true;
			controls.setOpacity(0);

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
	EventHandler<ActionEvent> updateHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent t) {
			if (reader != null && controls != null) {
				long position = getMediaPosition();

				controls.setLocation(position);
			}
		}
	};

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
	// User did something - show ui or mouse or whatever needs showing
	boolean mouseHiding;
	boolean mouseHidden = false;
	FadeTransition fadeout;
	FadeTransition fadein;
	EventHandler<ActionEvent> hideHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent t) {
			root.setCursor(Cursor.NONE);
			mouseHidden = true;

			if (fadein != null) {
				fadein.setRate(-1);
			} else {
				fadeout = new FadeTransition(Duration.millis(750), controls);
				fadeout.setFromValue(1);
				fadeout.setToValue(0);
				fadeout.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent t) {
						fadeout = null;
					}
				});
				fadeout.play();
			}
		}
	};
	Timeline mouseHide = new Timeline(new KeyFrame(Duration.seconds(2), hideHandler));

	void userActive() {

		// can probably just use one fade transition
		// and use playFrom + setdirection to manage it.

		if (mouseHidden) {
			// show mouse
			root.setCursor(Cursor.DEFAULT);
			if (fadeout != null) {
				fadeout.setRate(-1);
			} else if (fadein != null) {
				fadein.setRate(1);
			} else if (fadein == null) {
				fadein = new FadeTransition(Duration.millis(750), controls);
				fadein.setFromValue(0);
				fadein.setToValue(1);
				fadein.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent t) {
						fadein = null;
					}
				});
				fadein.play();
			}

			mouseHidden = false;
		} else {
			mouseHide.stop();
			mouseHide.play();
		}
	}

	@Override
	public long getMediaPosition() {
		long position = 0;

		if (haveAudio)
			position = aout.getPosition();
		else if (haveVideo)
			position = vout.getPosition();

		return position;
	}
}
