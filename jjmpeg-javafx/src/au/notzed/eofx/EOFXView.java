/*
 *  Copyright (c) 2013 Michael Zucchi
 *
 *  This programme is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This programme is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this programme.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.eofx;

import au.notzed.jjmpeg.mediaplayer.JFXMediaPlayer;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooserBuilder;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * Image viewer
 *
 * TODO: Dont' make this a stage, but put that on in the showImage static or something.
 * @author notzed
 */
public class EOFXView extends Stage {

	ScrollPane scroll;
	ImageView imageView;

	public EOFXView(Image image) {
		StackPane stack = new StackPane();

		scroll = new ScrollPane();
		scroll.setPannable(true);

		// The imageview must be inside a grup so that the scroll pane recognises it's size changing on zoom
		imageView = new ImageView(image);

		scroll.setContent(new Group(imageView));

		// Very simple zoom handler.
		scroll.addEventFilter(ScrollEvent.ANY,
				new EventHandler<ScrollEvent>() {
					double zoom = 1;

					@Override
					public void handle(ScrollEvent event) {
						if (event.getDeltaY() > 0) {
							zoom *= 1.1;
						} else if (event.getDeltaY() < 0) {
							zoom /= 1.1;
						}
						imageView.setScaleX(zoom);
						imageView.setScaleY(zoom);
						event.consume();
					}
				});

		BorderPane root = new BorderPane();

		// TODO: add buttons to add overlay stuff like timestamp, filename, and so on.

		HBox buttons = new HBox(2);

		// Can't for the life of me get this to offset nicely so it's stuck at some edge

		Button save = new Button("Save");
		buttons.getChildren().add(save);

		save.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				saveImage();
			}
		});

		Group g = new Group(buttons);

		StackPane.setAlignment(g, Pos.TOP_LEFT);
		buttons.setId("player-controls");
		stack.getChildren().addAll(scroll, g);
		root.setCenter(stack);

		Scene scene = new Scene(root, image.getWidth() + 2, image.getHeight() + 4);

		scene.getStylesheets().add("/au/notzed/jjmpeg/mediaplayer/style.css");
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN), new Runnable() {
			@Override
			public void run() {
				saveImage();
			}
		});
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE, KeyCodeCombination.SHORTCUT_ANY), new Runnable() {
			@Override
			public void run() {
				// Err, otherwise i get a gtk error
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						close();
					}
				});
			}
		});

		setTitle("Eye of FX");
		setScene(scene);
	}

	void saveImage() {
		Preferences prefs = Preferences.userNodeForPackage(JFXMediaPlayer.class);
		String drawer = prefs.get("save-drawer", null);
		File dir = drawer != null ? new File(drawer) : null;
		FileChooser fc = FileChooserBuilder.create().title("Save Frame").initialDirectory(dir).build();
		File file = fc.showSaveDialog(EOFXView.this);

		// TODO: write on thread
		if (file != null) {
			String name = file.getName().toLowerCase();
			prefs.put("save-drawer", file.getParent());

			String type = "png";
			int typei = name.lastIndexOf('.');
			if (typei != -1) {
				type = name.substring(typei + 1);
			}
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(imageView.getImage(), null), type, file);
			} catch (Exception ex) {
				Logger.getLogger(EOFXView.class.getName()).log(Level.SEVERE, null, ex);

				// FIXME: Err, reusable dialog error box thing

				final Stage stage = new Stage();
				stage.initModality(Modality.NONE);
				Button ok = new Button("Dismiss");
				stage.setScene(new Scene(VBoxBuilder.create().
						children(new Text("Error saving: " + ex.getLocalizedMessage()), ok).
						alignment(Pos.CENTER).padding(new Insets(5)).build()));
				stage.setResizable(false);
				stage.show();
				ok.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent t) {
						stage.close();
					}
				});
			}
		}
	}

	public void setImage(Image image) {
		// Set initial image
		imageView.setImage(image);
	}

	public static void showImage(Image image, String title) {
		EOFXView view = new EOFXView(image);

		view.show();
		view.setTitle(title);
	}
}
