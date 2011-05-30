// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVCodecContext64 extends AVCodecContext {
	AVCodecContext64(ByteBuffer p) {
		super(p);
	}
	public int getWidth() {
		return p.getInt(52);
	}

	public void setWidth(int val) {
		p.putInt(52, val);
	}

	public int getHeight() {
		return p.getInt(56);
	}

	public void setHeight(int val) {
		p.putInt(56, val);
	}

	public PixelFormat getPixFmt() {
		return PixelFormat.values()[p.getInt(64)+1];
	}

	public int getCodecType() {
		return p.getInt(264);
	}

	public int getCodecID() {
		return p.getInt(268);
	}

}
