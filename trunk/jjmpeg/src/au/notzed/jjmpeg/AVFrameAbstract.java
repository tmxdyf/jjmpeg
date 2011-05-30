// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVFrameAbstract {
	final ByteBuffer p;
	AVFrameAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public int getlineSizeAt(int i);
	protected final int dataOffset = 0;
}
