// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVFrame32 extends AVFrame {
	AVFrame32(ByteBuffer p) {
		super(p);
	}
	public int getLineSizeAt(int i) {
		return p.getInt(16+i*4);
	}

	int getDataOffset() {
		return 0;
	}

}
