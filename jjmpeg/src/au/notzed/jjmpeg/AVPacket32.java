// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVPacket32 extends AVPacket {
	AVPacket32(ByteBuffer p) {
		super(p);
	}
	public long getPTS() {
		return p.getLong(0);
	}

	public long getDTS() {
		return p.getLong(8);
	}

	public int getSize() {
		return p.getInt(20);
	}

	public int getStreamIndex() {
		return p.getInt(24);
	}

}
