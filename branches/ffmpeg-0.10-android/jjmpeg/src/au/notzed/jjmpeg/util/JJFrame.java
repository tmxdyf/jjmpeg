package au.notzed.jjmpeg.util;

import java.nio.ByteBuffer;

/**
 *
 * @author notzed
 */
public interface JJFrame {
	public ByteBuffer getBuffer(int plane);
	public int getLineSize(int plane);
	public void recycle();

}
