/* I am automatically generated.  Editing me would be pointless,
   but I wont stop you if you so desire. */

package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
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
	public native int getSampleRate();
	public native int setSampleRate(int val);
	public native int getChannels();
	public native int setChannels(int val);
	private native int _getSampleFmt();
	public  SampleFormat getSampleFmt() {
		return SampleFormat.values()[_getSampleFmt()+1];
	}
	private native int _setSampleFmt(int val);
	public  void setSampleFmt(SampleFormat val) {
		_setSampleFmt(val.toC());
	}
	public native int setFrameSize(int val);
	public native int getFrameNumber();
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
	public native int getStrictStdCompliance();
	public native int setStrictStdCompliance(int val);
	public native int getErrorRecognition();
	public native int setErrorRecognition(int val);
	public native int getIdctAlgo();
	public native int setIdctAlgo(int val);
	public native int getErrorConcealment();
	public native int setErrorConcealment(int val);
	// Native Methods
	native int _open(ByteBuffer codec);
	native int _close();
	native int _decode_video2(ByteBuffer picture, IntBuffer got_picture_ptr, ByteBuffer avpkt);
	native int _encode_video(ByteBuffer buf, int buf_size, ByteBuffer pict);
	native int _decode_audio3(ShortBuffer samples, IntBuffer frame_size_ptr, ByteBuffer avpkt);
	native int _encode_audio(ByteBuffer buf, int buf_size, ShortBuffer samples);
	native void _flush_buffers();
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
	public int encodeAudio(ByteBuffer buf, int buf_size, ShortBuffer samples) {
		return _encode_audio(buf, buf_size, samples);
	}
	public void flushBuffers() {
		_flush_buffers();
	}
	static public AVCodecContext allocContext() {
		return AVCodecContext.create(_alloc_context());
	}
	static public void init() {
		_init();
	}
}
abstract class AVCodecAbstract extends AVNative {
	protected AVCodecAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native String getName();
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
abstract class AVFormatContextAbstract extends AVNative {
	protected AVFormatContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	private native ByteBuffer _getInputFormat();
	public  AVInputFormat getInputFormat() {
		return AVInputFormat.create(_getInputFormat());
	}
	private native ByteBuffer _getOutputFormat();
	public  AVOutputFormat getOutputFormat() {
		return AVOutputFormat.create(_getOutputFormat());
	}
	public native int getNBStreams();
	private native ByteBuffer _getStreamAt(int index);
	public  AVStream getStreamAt(int index) {
		return AVStream.create(_getStreamAt(index));
	}
	public native long getStartTime();
	public native long getDuration();
	public native long getFileSize();
	public native int getBitRate();
	public native int getFlags();
	public native int setFlags(int val);
	// Native Methods
	native void _close_input_file();
	native int _seek_frame(int stream_index, long timestamp, int flags);
	native int _read_frame(ByteBuffer pkt);
	native int _find_stream_info();
	static native void _register_all();
	native int _seek_file(int stream_index, long min_ts, long ts, long max_ts, int flags);
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
	public int seekFile(int stream_index, long min_ts, long ts, long max_ts, int flags) {
		return _seek_file(stream_index, min_ts, ts, max_ts, flags);
	}
}
abstract class AVInputFormatAbstract extends AVNative {
	protected AVInputFormatAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native String getName();
	public native String getLongName();
	// Native Methods
	// Public Methods
}
abstract class AVOutputFormatAbstract extends AVNative {
	protected AVOutputFormatAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native String getName();
	public native String getLongName();
	public native String getMimeType();
	public native String getExtensions();
	// Native Methods
	// Public Methods
}
abstract class AVPacketAbstract extends AVNative {
	protected AVPacketAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	public native long getPTS();
	public native long setPTS(long val);
	public native long getDTS();
	public native long setDTS(long val);
	public native int getSize();
	public native int getStreamIndex();
	public native long getPos();
	public native int getFlags();
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
	public native long getPTS();
	public native long setPTS(long val);
	public native int getDisplayPictureNumber();
	public native int getCodedPictureNumber();
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
	public native long getNBFrames();
	public native long getStartTime();
	public native long getDuration();
	private native ByteBuffer _getRFrameRate();
	public  AVRational getRFrameRate() {
		return AVRational.create(_getRFrameRate());
	}
	private native ByteBuffer _getTimeBase();
	public  AVRational getTimeBase() {
		return AVRational.create(_getTimeBase());
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
	static public SwsContext getContext(int srcW, int srcH, PixelFormat srcFormat, int dstW, int dstH, PixelFormat dstFormat, int flags, SwsFilter srcFilter, SwsFilter dstFilter, DoubleBuffer param) {
		return SwsContext.create(_getContext(srcW, srcH, srcFormat.toC(), dstW, dstH, dstFormat.toC(), flags, srcFilter.p, dstFilter.p, param));
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
abstract class ReSampleContextAbstract extends AVNative {
	protected ReSampleContextAbstract(ByteBuffer p) {
		super(p);
	}
	// Fields
	// Native Methods
	static native ByteBuffer _resample_init(int output_channels, int input_channels, int output_rate, int input_rate, int sample_fmt_out, int sample_fmt_in, int filter_length, int log2_phase_count, int linear, double cutoff);
	native int _resample(ShortBuffer output, ShortBuffer input, int nb_samples);
	native void _resample_close();
	// Public Methods
	static public ReSampleContext resampleInit(int output_channels, int input_channels, int output_rate, int input_rate, SampleFormat sample_fmt_out, SampleFormat sample_fmt_in, int filter_length, int log2_phase_count, int linear, double cutoff) {
		return ReSampleContext.create(_resample_init(output_channels, input_channels, output_rate, input_rate, sample_fmt_out.toC(), sample_fmt_in.toC(), filter_length, log2_phase_count, linear, cutoff));
	}
	public void resampleClose() {
		_resample_close();
	}
}
