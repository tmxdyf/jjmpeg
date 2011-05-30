// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVPacketAbstract {
	final ByteBuffer p;
	AVPacketAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public long getPTS();
	abstract public long getDTS();
	abstract public int getSize();
	abstract public int getStreamIndex();
}
