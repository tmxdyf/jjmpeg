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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * TODO: this should be separate from the player and just call it via a MediaPlayer interface.
 * @author notzed
 */
public class JFXMediaControls extends VBox {

	JFXMediaPlayer player;
	Slider position;
	Label time;
	Label length;
	Button start;
	Button back;
	ToggleButton play;
	Button forward;
	Button end;
	boolean updatePosition;

	public JFXMediaControls() {

		position = new Slider();

		HBox hbox = new HBox();

		time = new Label("00:00:00");
		length = new Label("00:00:00");
		start = new Button("|<<");
		back = new Button(".<");
		play = new ToggleButton("|>");
		forward = new Button(">.");
		end = new Button(">>|");

		hbox.getChildren().addAll(time, start, back, play, forward, end, length);
		HBox.setHgrow(time, Priority.ALWAYS);
		HBox.setHgrow(length, Priority.ALWAYS);
		hbox.setAlignment(Pos.CENTER);

		getChildren().addAll(hbox, position);

		// FIXME: track state of player.
		play.setSelected(true);

		play.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				if (t1) {
					player.reader.play();
				} else {
					player.reader.pause();
				}
			}
		});

		start.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				player.reader.seek(0);
			}
		});
		end.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				player.reader.seek(player.reader.getDuration());
			}
		});
		back.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				player.reader.seek(Math.max(0, (long)position.getValue() - 5000));
			}
		});
		forward.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				player.reader.seek(Math.min(player.reader.getDuration(), (long)position.getValue() + 5000));
			}
		});

		position.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
				if (!updatePosition) {
					if (player != null
							&& player.reader != null) {
						player.reader.seek(t1.longValue());
					}
				}
			}
		});
	}

	public void setPlayer(JFXMediaPlayer player) {
		this.player = player;
	}

	String msToString(long ms) {
		long s = (ms / 1000) % 60;
		long m = (ms / 60000) % 60;
		long h = ms / 3600000;

		return String.format("%02d:%02d:%02d", h, m, s);
	}

	public void setDuration(long duration) {
		length.setText(msToString(duration));

		position.setMax(duration);
	}

	public void setLocation(long ms) {
		time.setText(msToString(ms));

		if (!updatePosition) {
			updatePosition = true;
			position.setValue(ms);
			updatePosition = false;
		}
	}
}
