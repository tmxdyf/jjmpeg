/* I am automatically generated.  Editing me would be pointless,
   but I wont stop you if you so desire. */

package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.DoubleBuffer;

abstract class AVCodecContextNativeAbstract extends AVNative {
	protected AVCodecContextNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native int getBitRate();
	native void setBitRate(int val);
	native int getFlags();
	native void setFlags(int val);
	native int getWidth();
	native void setWidth(int val);
	native int getHeight();
	native void setHeight(int val);
	native int getPixFmt();
	native void setPixFmt(int val);
	native int getSampleRate();
	native void setSampleRate(int val);
	native int getChannels();
	native void setChannels(int val);
	native long getChannelLayout();
	native void setChannelLayout(long val);
	native int getSampleFmt();
	native void setSampleFmt(int val);
	native int getFrameSize();
	native void setFrameSize(int val);
	native int getFrameNumber();
	native int getCodecType();
	native void setCodecType(int val);
	native int getCodecID();
	native void setCodecID(int val);
	native int getGOPSize();
	native void setGOPSize(int val);
	native int getMaxBFrames();
	native void setMaxBFrames(int val);
	native AVRational getTimeBase();
	native int getTimeBaseNum();
	native void setTimeBaseNum(int val);
	native int getTimeBaseDen();
	native void setTimeBaseDen(int val);
	native int getStrictStdCompliance();
	native void setStrictStdCompliance(int val);
	native int getIdctAlgo();
	native void setIdctAlgo(int val);
	native int getErrorConcealment();
	native void setErrorConcealment(int val);
	native int getMbDecision();
	native void setMbDecision(int val);
	native int getThreadCount();
	native void setThreadCount(int val);
	native int getSkipFrame();
	native void setSkipFrame(int val);
	native int getLowres();
	native void setLowres(int val);
	native int getCodedWidth();
	native void setCodedWidth(int val);
	native int getCodedHeight();
	native void setCodedHeight(int val);
	native AVFrame getCodedFrame();
	// Native Methods
	 native int open(AVCodecNative codec);
	 native int close();
	 native int decode_video2(AVFrameNative picture, IntBuffer got_picture_ptr, AVPacketNative avpkt);
	 native int encode_video(ByteBuffer buf, int buf_size, AVFrameNative pict);
	 native int decode_audio3(ShortBuffer samples, IntBuffer frame_size_ptr, AVPacketNative avpkt);
	 native int decode_audio4(AVFrameNative frame, IntBuffer got_frame_ptr, AVPacketNative avpkt);
	 native int encode_audio(ByteBuffer buf, int buf_size, ShortBuffer samples);
	 native void flush_buffers();
	static  native AVCodecContext alloc_context();
}

abstract class AVCodecContextAbstract extends AVObject {
	AVCodecContextNative n;
	final protected void setNative(AVCodecContextNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  int getBitRate() {
		return n.getBitRate();
	}
	public  void setBitRate(int val) {
		n.setBitRate(val);
	}
	public  int getFlags() {
		return n.getFlags();
	}
	public  void setFlags(int val) {
		n.setFlags(val);
	}
	public  int getWidth() {
		return n.getWidth();
	}
	public  void setWidth(int val) {
		n.setWidth(val);
	}
	public  int getHeight() {
		return n.getHeight();
	}
	public  void setHeight(int val) {
		n.setHeight(val);
	}
	public  PixelFormat getPixFmt() {
		return PixelFormat.values()[n.getPixFmt()+1];
	}
	public  void setPixFmt(PixelFormat val) {
		n.setPixFmt(val.toC());
	}
	public  int getSampleRate() {
		return n.getSampleRate();
	}
	public  void setSampleRate(int val) {
		n.setSampleRate(val);
	}
	public  int getChannels() {
		return n.getChannels();
	}
	public  void setChannels(int val) {
		n.setChannels(val);
	}
	public  long getChannelLayout() {
		return n.getChannelLayout();
	}
	public  void setChannelLayout(long val) {
		n.setChannelLayout(val);
	}
	public  AVSampleFormat getSampleFmt() {
		return AVSampleFormat.values()[n.getSampleFmt()+1];
	}
	public  void setSampleFmt(AVSampleFormat val) {
		n.setSampleFmt(val.toC());
	}
	public  int getFrameSize() {
		return n.getFrameSize();
	}
	public  void setFrameSize(int val) {
		n.setFrameSize(val);
	}
	public  int getFrameNumber() {
		return n.getFrameNumber();
	}
	public  int getCodecType() {
		return n.getCodecType();
	}
	public  void setCodecType(int val) {
		n.setCodecType(val);
	}
	public  int getCodecID() {
		return n.getCodecID();
	}
	public  void setCodecID(int val) {
		n.setCodecID(val);
	}
	public  int getGOPSize() {
		return n.getGOPSize();
	}
	public  void setGOPSize(int val) {
		n.setGOPSize(val);
	}
	public  int getMaxBFrames() {
		return n.getMaxBFrames();
	}
	public  void setMaxBFrames(int val) {
		n.setMaxBFrames(val);
	}
	public  AVRational getTimeBase() {
		return n.getTimeBase();
	}
	public  int getTimeBaseNum() {
		return n.getTimeBaseNum();
	}
	public  void setTimeBaseNum(int val) {
		n.setTimeBaseNum(val);
	}
	public  int getTimeBaseDen() {
		return n.getTimeBaseDen();
	}
	public  void setTimeBaseDen(int val) {
		n.setTimeBaseDen(val);
	}
	public  int getStrictStdCompliance() {
		return n.getStrictStdCompliance();
	}
	public  void setStrictStdCompliance(int val) {
		n.setStrictStdCompliance(val);
	}
	public  int getIdctAlgo() {
		return n.getIdctAlgo();
	}
	public  void setIdctAlgo(int val) {
		n.setIdctAlgo(val);
	}
	public  int getErrorConcealment() {
		return n.getErrorConcealment();
	}
	public  void setErrorConcealment(int val) {
		n.setErrorConcealment(val);
	}
	public  int getMbDecision() {
		return n.getMbDecision();
	}
	public  void setMbDecision(int val) {
		n.setMbDecision(val);
	}
	public  int getThreadCount() {
		return n.getThreadCount();
	}
	public  void setThreadCount(int val) {
		n.setThreadCount(val);
	}
	public  int getSkipFrame() {
		return n.getSkipFrame();
	}
	public  void setSkipFrame(int val) {
		n.setSkipFrame(val);
	}
	public  int getLowres() {
		return n.getLowres();
	}
	public  void setLowres(int val) {
		n.setLowres(val);
	}
	public  int getCodedWidth() {
		return n.getCodedWidth();
	}
	public  void setCodedWidth(int val) {
		n.setCodedWidth(val);
	}
	public  int getCodedHeight() {
		return n.getCodedHeight();
	}
	public  void setCodedHeight(int val) {
		n.setCodedHeight(val);
	}
	public  AVFrame getCodedFrame() {
		return n.getCodedFrame();
	}
	// Public Methods
	public int close() {
		return n.close();
	}
	 int decodeVideo2(AVFrame picture, IntBuffer got_picture_ptr, AVPacket avpkt) {
		return n.decode_video2(picture != null ? picture.n : null, got_picture_ptr, avpkt != null ? avpkt.n : null);
	}
	public int encodeVideo(ByteBuffer buf, int buf_size, AVFrame pict) {
		return n.encode_video(buf, buf_size, pict != null ? pict.n : null);
	}
	 int decodeAudio3(ShortBuffer samples, IntBuffer frame_size_ptr, AVPacket avpkt) {
		return n.decode_audio3(samples, frame_size_ptr, avpkt != null ? avpkt.n : null);
	}
	 int decodeAudio4(AVFrame frame, IntBuffer got_frame_ptr, AVPacket avpkt) {
		return n.decode_audio4(frame != null ? frame.n : null, got_frame_ptr, avpkt != null ? avpkt.n : null);
	}
	public int encodeAudio(ByteBuffer buf, int buf_size, ShortBuffer samples) {
		return n.encode_audio(buf, buf_size, samples);
	}
	public void flushBuffers() {
		n.flush_buffers();
	}
	static protected AVCodecContext allocContext() {
		return AVCodecContextNativeAbstract.alloc_context();
	}
}
abstract class AVCodecNativeAbstract extends AVNative {
	protected AVCodecNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native String getName();
	// Native Methods
	static  native AVCodec find_encoder(int id);
	static  native AVCodec find_decoder(int id);
	static  native AVCodec find_encoder_by_name(String name);
}

abstract class AVCodecAbstract extends AVObject {
	AVCodecNative n;
	final protected void setNative(AVCodecNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  String getName() {
		return n.getName();
	}
	// Public Methods
	static public AVCodec findEncoder(int id) {
		return AVCodecNativeAbstract.find_encoder(id);
	}
	static public AVCodec findDecoder(int id) {
		return AVCodecNativeAbstract.find_decoder(id);
	}
	static public AVCodec findEncoderByName(String name) {
		return AVCodecNativeAbstract.find_encoder_by_name(name);
	}
}
abstract class AVFormatContextNativeAbstract extends AVNative {
	protected AVFormatContextNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native AVInputFormat getInputFormat();
	native void setInputFormat(AVInputFormatNative val);
	native AVOutputFormat getOutputFormat();
	native void setOutputFormat(AVOutputFormatNative val);
	native AVIOContext getIOContext();
	native void setIOContext(AVIOContextNative val);
	native int getNBStreams();
	native AVStream getStreamAt(int index);
	native long getStartTime();
	native long getDuration();
	native int getBitRate();
	native int getFlags();
	native void setFlags(int val);
	native int getProbesize();
	native void setProbesize(int val);
	native int getMaxAnalyzeDuration();
	native void setMaxAnalyzeDuration(int val);
	// Native Methods
	 native int seek_frame(int stream_index, long timestamp, int flags);
	 native int read_frame(AVPacketNative pkt);
	 native int write_frame(AVPacketNative pkt);
	 native int interleaved_write_frame(AVPacketNative pkt);
	 native int write_trailer();
	static  native void register_all();
	 native AVStream new_stream(AVCodecNative codec);
	 native int seek_file(int stream_index, long min_ts, long ts, long max_ts, int flags);
	static  native AVFormatContext alloc_context();
	 native void free_context();
	static  native int network_init();
}

abstract class AVFormatContextAbstract extends AVObject {
	AVFormatContextNative n;
	final protected void setNative(AVFormatContextNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  AVInputFormat getInputFormat() {
		return n.getInputFormat();
	}
	public  void setInputFormat(AVInputFormat val) {
		n.setInputFormat(val.n);
	}
	public  AVOutputFormat getOutputFormat() {
		return n.getOutputFormat();
	}
	public  void setOutputFormat(AVOutputFormat val) {
		n.setOutputFormat(val.n);
	}
	public  AVIOContext getIOContext() {
		return n.getIOContext();
	}
	public  void setIOContext(AVIOContext val) {
		n.setIOContext(val.n);
	}
	public  int getNBStreams() {
		return n.getNBStreams();
	}
	public  AVStream getStreamAt(int index) {
		return n.getStreamAt(index);
	}
	public  long getStartTime() {
		return n.getStartTime();
	}
	public  long getDuration() {
		return n.getDuration();
	}
	public  int getBitRate() {
		return n.getBitRate();
	}
	public  int getFlags() {
		return n.getFlags();
	}
	public  void setFlags(int val) {
		n.setFlags(val);
	}
	public  int getProbesize() {
		return n.getProbesize();
	}
	public  void setProbesize(int val) {
		n.setProbesize(val);
	}
	public  int getMaxAnalyzeDuration() {
		return n.getMaxAnalyzeDuration();
	}
	public  void setMaxAnalyzeDuration(int val) {
		n.setMaxAnalyzeDuration(val);
	}
	// Public Methods
	public int seekFrame(int stream_index, long timestamp, int flags) {
		return n.seek_frame(stream_index, timestamp, flags);
	}
	public int readFrame(AVPacket pkt) {
		return n.read_frame(pkt != null ? pkt.n : null);
	}
	public int writeFrame(AVPacket pkt) {
		return n.write_frame(pkt != null ? pkt.n : null);
	}
	public int writeTrailer() {
		return n.write_trailer();
	}
	static public void registerAll() {
		AVFormatContextNativeAbstract.register_all();
	}
	public AVStream newStream(AVCodec codec) {
		return n.new_stream(codec != null ? codec.n : null);
	}
	public int seekFile(int stream_index, long min_ts, long ts, long max_ts, int flags) {
		return n.seek_file(stream_index, min_ts, ts, max_ts, flags);
	}
	static public AVFormatContext allocContext() {
		return AVFormatContextNativeAbstract.alloc_context();
	}
	protected void freeContext() {
		n.free_context();
	}
	static public int networkInit() {
		return AVFormatContextNativeAbstract.network_init();
	}
}
abstract class AVInputFormatNativeAbstract extends AVNative {
	protected AVInputFormatNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native String getName();
	native String getLongName();
	native int getFlags();
	// Native Methods
	static  native AVInputFormat find_input_format(String short_name);
}

abstract class AVInputFormatAbstract extends AVObject {
	AVInputFormatNative n;
	final protected void setNative(AVInputFormatNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  String getName() {
		return n.getName();
	}
	public  String getLongName() {
		return n.getLongName();
	}
	public  int getFlags() {
		return n.getFlags();
	}
	// Public Methods
	static public AVInputFormat findInputFormat(String short_name) {
		return AVInputFormatNativeAbstract.find_input_format(short_name);
	}
}
abstract class AVOutputFormatNativeAbstract extends AVNative {
	protected AVOutputFormatNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native String getName();
	native String getLongName();
	native String getMimeType();
	native String getExtensions();
	native int getVideoCodec();
	native int getAudioCodec();
	native int getSubtitleCodec();
	native int getFlags();
	native void setFlags(int val);
	// Native Methods
	static  native AVOutputFormat guess_format(String short_name, String filename, String mime_type);
}

abstract class AVOutputFormatAbstract extends AVObject {
	AVOutputFormatNative n;
	final protected void setNative(AVOutputFormatNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  String getName() {
		return n.getName();
	}
	public  String getLongName() {
		return n.getLongName();
	}
	public  String getMimeType() {
		return n.getMimeType();
	}
	public  String getExtensions() {
		return n.getExtensions();
	}
	public  int getVideoCodec() {
		return n.getVideoCodec();
	}
	public  int getAudioCodec() {
		return n.getAudioCodec();
	}
	public  int getSubtitleCodec() {
		return n.getSubtitleCodec();
	}
	public  int getFlags() {
		return n.getFlags();
	}
	public  void setFlags(int val) {
		n.setFlags(val);
	}
	// Public Methods
	static public AVOutputFormat guessFormat(String short_name, String filename, String mime_type) {
		return AVOutputFormatNativeAbstract.guess_format(short_name, filename, mime_type);
	}
}
abstract class AVFormatParametersNativeAbstract extends AVNative {
	protected AVFormatParametersNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
}

abstract class AVFormatParametersAbstract extends AVObject {
	AVFormatParametersNative n;
	final protected void setNative(AVFormatParametersNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
}
abstract class AVPacketNativeAbstract extends AVNative {
	protected AVPacketNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native long getPTS();
	native void setPTS(long val);
	native long getDTS();
	native void setDTS(long val);
	native int getSize();
	native int getStreamIndex();
	native void setStreamIndex(int val);
	native long getPos();
	native int getFlags();
	native void setFlags(int val);
	// Native Methods
	 native void free_packet();
	 native void init_packet();
	 native int dup_packet();
}

abstract class AVPacketAbstract extends AVObject {
	AVPacketNative n;
	final protected void setNative(AVPacketNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  long getPTS() {
		return n.getPTS();
	}
	public  void setPTS(long val) {
		n.setPTS(val);
	}
	public  long getDTS() {
		return n.getDTS();
	}
	public  void setDTS(long val) {
		n.setDTS(val);
	}
	public  int getSize() {
		return n.getSize();
	}
	public  int getStreamIndex() {
		return n.getStreamIndex();
	}
	public  void setStreamIndex(int val) {
		n.setStreamIndex(val);
	}
	public  long getPos() {
		return n.getPos();
	}
	public  int getFlags() {
		return n.getFlags();
	}
	public  void setFlags(int val) {
		n.setFlags(val);
	}
	// Public Methods
	public void freePacket() {
		n.free_packet();
	}
	public void initPacket() {
		n.init_packet();
	}
	public int dupPacket() {
		return n.dup_packet();
	}
}
abstract class AVFrameNativeAbstract extends AVNative {
	protected AVFrameNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native int getLineSizeAt(int index);
	native int getKeyFrame();
	native long getPTS();
	native void setPTS(long val);
	native int getDisplayPictureNumber();
	native int getCodedPictureNumber();
	native int getNbSamples();
	native void setNbSamples(int val);
	// Native Methods
	static  native AVFrame alloc_frame();
	 native void get_frame_defaults();
	 native int fill_audio_frame(int nb_channels, int sample_fmt, ByteBuffer buf, int buf_size, int align);
	 native int alloc(int pix_fmt, int width, int height);
	 native void free();
	 native void copy(AVFrameNative src, int fmt, int width, int height);
	 native long get_best_effort_timestamp();
}

abstract class AVFrameAbstract extends AVObject {
	AVFrameNative n;
	final protected void setNative(AVFrameNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  int getLineSizeAt(int index) {
		return n.getLineSizeAt(index);
	}
	public  int getKeyFrame() {
		return n.getKeyFrame();
	}
	public  long getPTS() {
		return n.getPTS();
	}
	public  void setPTS(long val) {
		n.setPTS(val);
	}
	public  int getDisplayPictureNumber() {
		return n.getDisplayPictureNumber();
	}
	public  int getCodedPictureNumber() {
		return n.getCodedPictureNumber();
	}
	public  int getNbSamples() {
		return n.getNbSamples();
	}
	public  void setNbSamples(int val) {
		n.setNbSamples(val);
	}
	// Public Methods
	public void getFrameDefaults() {
		n.get_frame_defaults();
	}
	public int fillAudioFrame(int nb_channels, AVSampleFormat sample_fmt, ByteBuffer buf, int buf_size, int align) {
		return n.fill_audio_frame(nb_channels, sample_fmt.toC(), buf, buf_size, align);
	}
	public int alloc(int pix_fmt, int width, int height) {
		return n.alloc(pix_fmt, width, height);
	}
	public void free() {
		n.free();
	}
	public void copy(AVFrame src, PixelFormat fmt, int width, int height) {
		n.copy(src != null ? src.n : null, fmt.toC(), width, height);
	}
	public long getBestEffortTimestamp() {
		return n.get_best_effort_timestamp();
	}
}
abstract class AVStreamNativeAbstract extends AVNative {
	protected AVStreamNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native int getIndex();
	native int getId();
	native AVCodecContext getCodec();
	native AVRational getRFrameRate();
	native AVRational getTimeBase();
	native long getStartTime();
	native long getDuration();
	native long getNBFrames();
	// Native Methods
}

abstract class AVStreamAbstract extends AVObject {
	AVStreamNative n;
	final protected void setNative(AVStreamNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  int getIndex() {
		return n.getIndex();
	}
	public  int getId() {
		return n.getId();
	}
	public  AVCodecContext getCodec() {
		return n.getCodec();
	}
	public  AVRational getRFrameRate() {
		return n.getRFrameRate();
	}
	public  AVRational getTimeBase() {
		return n.getTimeBase();
	}
	public  long getStartTime() {
		return n.getStartTime();
	}
	public  long getDuration() {
		return n.getDuration();
	}
	public  long getNBFrames() {
		return n.getNBFrames();
	}
	// Public Methods
}
abstract class AVRationalNativeAbstract extends AVNative {
	protected AVRationalNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	native int getNum();
	native void setNum(int val);
	native int getDen();
	native void setDen(int val);
	// Native Methods
	static  native long rescale(long a, long b, long c);
}

abstract class AVRationalAbstract extends AVObject {
	AVRationalNative n;
	final protected void setNative(AVRationalNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	public  int getNum() {
		return n.getNum();
	}
	public  void setNum(int val) {
		n.setNum(val);
	}
	public  int getDen() {
		return n.getDen();
	}
	public  void setDen(int val) {
		n.setDen(val);
	}
	// Public Methods
	static public long rescale(long a, long b, long c) {
		return AVRationalNativeAbstract.rescale(a, b, c);
	}
}
abstract class AVIOContextNativeAbstract extends AVNative {
	protected AVIOContextNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
	 native int close();
}

abstract class AVIOContextAbstract extends AVObject {
	AVIOContextNative n;
	final protected void setNative(AVIOContextNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
	public int close() {
		return n.close();
	}
}
abstract class SwsContextNativeAbstract extends AVNative {
	protected SwsContextNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
	static  native SwsContext getContext(int srcW, int srcH, int srcFormat, int dstW, int dstH, int dstFormat, int flags, SwsFilterNative srcFilter, SwsFilterNative dstFilter, DoubleBuffer param);
	 native void freeContext();
}

abstract class SwsContextAbstract extends AVObject {
	SwsContextNative n;
	final protected void setNative(SwsContextNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
	public void freeContext() {
		n.freeContext();
	}
}
abstract class SwsFilterNativeAbstract extends AVNative {
	protected SwsFilterNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
}

abstract class SwsFilterAbstract extends AVObject {
	SwsFilterNative n;
	final protected void setNative(SwsFilterNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
}
abstract class SwrContextNativeAbstract extends AVNative {
	protected SwrContextNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
	 native int set_compensation(int sample_delta, int compensation_distance);
}

abstract class SwrContextAbstract extends AVObject {
	SwrContextNative n;
	final protected void setNative(SwrContextNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
	public int setCompensation(int sample_delta, int compensation_distance) {
		return n.set_compensation(sample_delta, compensation_distance);
	}
}
abstract class AVDictionaryNativeAbstract extends AVNative {
	protected AVDictionaryNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
}

abstract class AVDictionaryAbstract extends AVObject {
	AVDictionaryNative n;
	final protected void setNative(AVDictionaryNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
}
abstract class AVDeviceNativeAbstract extends AVNative {
	protected AVDeviceNativeAbstract(AVObject o) {
		super(o);
	}
	// Fields
	// Native Methods
	static  native void register_all();
	static  native int version();
}

abstract class AVDeviceAbstract extends AVObject {
	AVDeviceNative n;
	final protected void setNative(AVDeviceNative n) {
		this.n = n;
	}
	public void dispose() {
		n.dispose();
	}
	// Fields
	// Public Methods
	static public void registerAll() {
		AVDeviceNativeAbstract.register_all();
	}
	static public int version() {
		return AVDeviceNativeAbstract.version();
	}
}
