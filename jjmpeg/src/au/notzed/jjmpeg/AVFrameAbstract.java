// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVFrameAbstract extends AVNative {
	final ByteBuffer p;
	AVFrameAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public int getLineSizeAt(int i);
	abstract int getDataOffset();
}
