// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVCodecContextAbstract extends AVNative {
	final ByteBuffer p;
	AVCodecContextAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public int getWidth();
	abstract public void setWidth(int val);
	abstract public int getHeight();
	abstract public void setHeight(int val);
	abstract public PixelFormat getPixFmt();
	abstract public int getCodecType();
	abstract public int getCodecID();
}
