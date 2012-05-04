package au.notzed.jjmpeg.util;

import java.nio.ByteBuffer;

/**
 *
 * @author notzed
 */
public interface JJFrame {
	public ByteBuffer getBuffer();
	public void recycle();

}
