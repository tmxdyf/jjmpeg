// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVStream32 extends AVStream {
	AVStream32(ByteBuffer p) {
		super(p);
	}
	public int getIndex() {
		return p.getInt(0);
	}

	public AVCodecContext getCodec() {
		return AVCodecContext.create(AVNative.getPointer(p, 8, 924));
	}

}
