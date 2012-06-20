package au.notzed.jjmpeg.util;

import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.PixelFormat;

/**
 *
 * @author notzed
 */
public interface JJFrame {

	public void recycle();

	public AVFrame getFrame();

	public PixelFormat getFormat();
}
