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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 * Video surface rendered as an icon via a BufferedImage.
 *
 * This just renders an icon the same size as it's container.
 * (probably should use swscale to do the scaling)
 * @author notzed
 */
public class IconSurface implements Icon {

	private final BufferedImage image;

	public IconSurface(BufferedImage image) {
		this.image = image;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.drawImage(image, x, y, c.getWidth(), c.getHeight(), null);
	}

	@Override
	public int getIconWidth() {
		return image.getWidth();
	}

	@Override
	public int getIconHeight() {
		return image.getHeight();
	}
}
