// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
class AVFormatContext64 extends AVFormatContext {
	AVFormatContext64(ByteBuffer p) {
		super(p);
	}
	public int getNBStreams() {
		return p.getInt(40);
	}

	public AVStream getStreamAt(int i) {
		return AVStream.create(AVNative.getPointerIndex(p, 48, 528, i));
	}

}
