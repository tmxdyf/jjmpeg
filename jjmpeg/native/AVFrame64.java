// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVFrame64 extends AVFrame {
	AVFrame64(ByteBuffer p) {
		super(p);
	}
	public int getLineSizeAt(int i) {
		return p.getInt(32+i*4);
	}

	int getDataOffset() {
		return 0;
	}

}
