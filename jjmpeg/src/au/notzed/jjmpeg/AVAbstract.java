/* I am automatically generated.  Editing me would be pointless,
   but I wont stop you if you so desire. */

package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.DoubleBuffer;

abstract class AVCodecContextAbstract extends AVNative {
	protected AVCodecContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getWidth();
	public native int setWidth(int val);
	public native int getHeight();
	public native int setHeight(int val);
	private native int _getPixFmt();
	public  PixelFormat getPixFmt() {
		return PixelFormat.values()[_getPixFmt()+1];
	}
	private native int _setPixFmt(int val);
	public  void setPixFmt(PixelFormat val) {
		_setPixFmt(val.toC());
	}
	public native int getCodecType();
	public native int getCodecID();
	public native int getBitRate();
	public native int setBitRate(int val);
	public native int getGOPSize();
	public native int setGOPSize(int val);
	public native int getMaxBFrames();
	public native int setMaxBFrames(int val);
	private native ByteBuffer _getTimeBase();
	public  AVRational getTimeBase() {
		return AVRational.create(_getTimeBase());
	}
	public native int getTimeBase_num();
	public native int setTimeBase_num(int val);
	public native int getTimeBase_den();
	public native int setTimeBase_den(int val);
	// Native Methods
	native int _open(ByteBuffer codec);
	native int _close();
	native int _decode_video2(ByteBuffer picture, IntBuffer got_picture_ptr, ByteBuffer avpkt);
	native int _encode_video(ByteBuffer buf, int buf_size, ByteBuffer pict);
	static native ByteBuffer _alloc_context();
	static native void _init();
	// Public Methods
	public int open(AVCodec codec) {
		return _open(codec.p);
	}
	public int close() {
		return _close();
	}
	public int encodeVideo(ByteBuffer buf, int buf_size, AVFrame pict) {
		return _encode_video(buf, buf_size, pict.p);
	}
	static public AVCodecContext allocContext() {
		return AVCodecContext.create(_alloc_context());
	}
	static public void init() {
		_init();
	}
}
abstract class AVFormatContextAbstract extends AVNative {
	protected AVFormatContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getNBStreams();
	private native ByteBuffer _getStreamAt(int index);
	public  AVStream getStreamAt(int index) {
		return AVStream.create(_getStreamAt(index));
	}
	// Native Methods
	native void _close_input_file();
	native int _seek_frame(int stream_index, long timestamp, int flags);
	native int _read_frame(ByteBuffer pkt);
	native int _find_stream_info();
	static native void _register_all();
	static native void _free(ByteBuffer mem);
	// Public Methods
	public void closeInputFile() {
		_close_input_file();
	}
	public int seekFrame(int stream_index, long timestamp, int flags) {
		return _seek_frame(stream_index, timestamp, flags);
	}
	public int readFrame(AVPacket pkt) {
		return _read_frame(pkt.p);
	}
	public int findStreamInfo() {
		return _find_stream_info();
	}
	static public void registerAll() {
		_register_all();
	}
}
abstract class AVCodecAbstract extends AVNative {
	protected AVCodecAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	// Native Methods
	static native ByteBuffer _find_encoder(int id);
	static native ByteBuffer _find_decoder(int id);
	static native ByteBuffer _find_encoder_by_name(String name);
	// Public Methods
	static public AVCodec findEncoder(int id) {
		return AVCodec.create(_find_encoder(id));
	}
	static public AVCodec findDecoder(int id) {
		return AVCodec.create(_find_decoder(id));
	}
	static public AVCodec findEncoderByName(String name) {
		return AVCodec.create(_find_encoder_by_name(name));
	}
}
abstract class AVPacketAbstract extends AVNative {
	protected AVPacketAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getPTS();
	public native int setPTS(int val);
	public native int getDTS();
	public native int setDTS(int val);
	public native int getSize();
	public native int getStreamIndex();
	// Native Methods
	native void _free_packet();
	// Public Methods
	public void freePacket() {
		_free_packet();
	}
}
abstract class AVFrameAbstract extends AVNative {
	protected AVFrameAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getLineSizeAt(int index);
	public native int getKeyFrame();
	// Native Methods
	static native ByteBuffer _alloc_frame();
	native int _alloc(int pix_fmt, int width, int height);
	native void _free();
	// Public Methods
	static public AVFrame allocFrame() {
		return AVFrame.create(_alloc_frame());
	}
	public int alloc(int pix_fmt, int width, int height) {
		return _alloc(pix_fmt, width, height);
	}
	public void free() {
		_free();
	}
}
abstract class AVStreamAbstract extends AVNative {
	protected AVStreamAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getIndex();
	private native ByteBuffer _getCodec();
	public  AVCodecContext getCodec() {
		return AVCodecContext.create(_getCodec());
	}
	// Native Methods
	// Public Methods
}
abstract class AVRationalAbstract extends AVNative {
	protected AVRationalAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native int getNum();
	public native int setNum(int val);
	public native int getDen();
	public native int setDen(int val);
	// Native Methods
	// Public Methods
}
abstract class ByteIOContextAbstract extends AVNative {
	protected ByteIOContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	// Native Methods
	// Public Methods
}
abstract class SwsContextAbstract extends AVNative {
	protected SwsContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	// Native Methods
	static native ByteBuffer _getContext(int srcW, int srcH, int srcFormat, int dstW, int dstH, int dstFormat, int flags, ByteBuffer srcFilter, ByteBuffer dstFilter, DoubleBuffer param);
	native void _freeContext();
	// Public Methods
	static public SwsContext getContext(int srcW, int srcH, int srcFormat, int dstW, int dstH, int dstFormat, int flags, SwsFilter srcFilter, SwsFilter dstFilter, DoubleBuffer param) {
		return SwsContext.create(_getContext(srcW, srcH, srcFormat, dstW, dstH, dstFormat, flags, srcFilter.p, dstFilter.p, param));
	}
	public void freeContext() {
		_freeContext();
	}
}
abstract class SwsFilterAbstract extends AVNative {
	protected SwsFilterAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	// Native Methods
	// Public Methods
}
