// Auto-generated, editing would be pointless
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
abstract class AVFormatContextAbstract {
	final ByteBuffer p;
	AVFormatContextAbstract(ByteBuffer p) {
		this.p = p;
		p.order(ByteOrder.nativeOrder());
	}
	abstract public int getNBStreams();
	abstract public AVStream getStreamAt(int i);
}
