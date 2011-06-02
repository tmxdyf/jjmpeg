// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVStreamAbstract extends AVNative {
	final ByteBuffer p;
	AVStreamAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public int getIndex();
	abstract public AVCodecContext getCodec();
}
