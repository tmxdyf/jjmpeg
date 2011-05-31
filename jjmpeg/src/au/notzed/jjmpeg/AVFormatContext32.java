// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVFormatContext32 extends AVFormatContext {
	AVFormatContext32(ByteBuffer p) {
		super(p);
	}
	public int getNBStreams() {
		return p.getInt(20);
	}

	public AVStream getStreamAt(int i) {
		return AVStream.create(AVNative.getPointerIndex(p, 24, 452, i));
	}

}
