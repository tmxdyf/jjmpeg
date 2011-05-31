// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVCodecContext32 extends AVCodecContext {
	AVCodecContext32(ByteBuffer p) {
		super(p);
	}
	public int getWidth() {
		return p.getInt(40);
	}

	public void setWidth(int val) {
		p.putInt(40, val);
	}

	public int getHeight() {
		return p.getInt(44);
	}

	public void setHeight(int val) {
		p.putInt(44, val);
	}

	public PixelFormat getPixFmt() {
		return PixelFormat.values()[p.getInt(52)+1];
	}

	public int getCodecType() {
		return p.getInt(220);
	}

	public int getCodecID() {
		return p.getInt(224);
	}

}
