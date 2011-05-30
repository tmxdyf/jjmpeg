// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVFrame64 extends AVFrame {
	AVFrame64(ByteBuffer p) {
		super(p);
	}
	public int getlineSizeAt(int i) {
		return p.getInt(32+i*4);
	}

}
