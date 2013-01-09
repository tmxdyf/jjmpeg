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

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;

/**
 *
 * @author notzed
 */
public class JFXIcons {

	static Shape style(Shape s) {
		s.setStrokeWidth(2.5);
		s.setStroke(Color.BLACK);
		s.setFill(null);
		return s;
	}

	static public Node pause() {
		Rectangle r1 = new Rectangle(111.5, 57.362183, 5, 30);
		Rectangle r2 = new Rectangle(123.5, 57.362183, 5, 30);

		style(r1);
		style(r1);
		return new Group(r1, r2);
	}

	static public Shape play() {
		SVGPath path = new SVGPath();
		String d = "M 20,9.2626404 C 18.441154,8.396615 18.176348,-7.6504997 19.705771,-8.5674871 21.235194,-9.4844745 35.264806,-1.6902461 35.294229,0.09276669 35.323652,1.8757794 21.558846,10.128666 20,9.2626404 z";



		path.setContent(d);

//				"m 97.000001,116.36218 c -2.424872,-1.38564 -2.536793,-26.521021 -0.124357,-27.9282 2.412435,-1.40718 24.236276,11.063585 24.248716,13.85641 0.0124,2.79282 -21.699488,15.45743 -24.124359,14.07179 z");
		return style(path);
	}

	static public Node finish() {
		Shape p1 = play();
		Shape p2 = play();
		Rectangle r = new Rectangle(0, -10, 5, 20);

		r.setArcHeight(5);
		r.setArcWidth(5);

		style(r);
		p1.setTranslateX(0);
		p2.setTranslateX(9);
		r.setTranslateX(48);

		return new Group(p1, p2, r);
	}

	static public Node start() {
		Node n = finish();
		n.setScaleX(-1);
		return n;
	}

	static public Node skipForward() {
		Shape p1 = play();
		Shape p2 = play();
		Ellipse d = new Ellipse(0, 0, 3, 3);

		style(d);
		p1.setTranslateX(0);
		p2.setTranslateX(9);
		d.setTranslateX(48+3);

		return new Group(p1, p2, d);
	}
	static public Node skipBackward() {
		Node n = skipForward();
		n.setScaleX(-1);
		return n;
	}
}
