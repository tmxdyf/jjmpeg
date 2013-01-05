/*
 * Copyright (c) 2012 Michael Zucchi
 *
 * This file is part of jjmpeg.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg.mediaplayer;

import au.notzed.jjmpeg.util.CancellableThread;
import au.notzed.jjmpeg.AVCodecContext;
import au.notzed.jjmpeg.AVFormatContext;
import au.notzed.jjmpeg.AVPacket;
import au.notzed.jjmpeg.AVStream;
import au.notzed.jjmpeg.util.JJQueue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low Level Media Player
 *
 * @author notzed
 */
public class MediaReader extends CancellableThread implements MediaPlayer {

	static final int packetLimit = 61;
	AVFormatContext format;
	HashMap<Integer, MediaDecoder> streamMap = new HashMap<Integer, MediaDecoder>();
	String file;
	MediaSink dest;
	// for recycling packets
	JJQueue<AVPacket> packetQueue = new JJQueue<AVPacket>(packetLimit + 5);
	// for commands to the player thread
	LinkedBlockingQueue<PlayerCMD> cmdQueue = new LinkedBlockingQueue<PlayerCMD>();
	long duration;
	MediaListener listener;
	MediaState state = MediaState.Idle;

	public MediaReader() {
		super("AVReader");
	}

	public MediaReader(String fileName) throws IOException {
		super("AVReader: " + fileName);

		this.file = fileName;

		format = AVFormatContext.open(file);
		//format.setProbesize(1024 * 1024);
		//format.setMaxAnalyzeDuration(1000000);
		format.findStreamInfo();

		state = MediaState.Idle;
	}

	public String getPath() {
		return file;
	}

	/**
	 * May only be called in the paused/playing state.
	 * @return
	 */
	public Set<Entry<Integer, MediaDecoder>> getDecoders() {
		return streamMap.entrySet();
	}

	@Override
	public void setListener(MediaListener l) {
		this.listener = l;
	}

	@Override
	public void play() {
		cmdQueue.offer(new PlayerCMD(PlayerCMD.PLAY));
	}

	@Override
	public void seek(long ms) {
		seek(ms, 0);
	}

	@Override
	public long getPosition() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public MediaState getMediaState() {
		return state;
	}

	class PlayerCMD {

		int cmd;
		long stamp;
		String name;
		final static int QUIT = 0;
		final static int PLAY = 1;
		final static int SEEK = 2;
		final static int PAUSE = 3;
		final static int RESUME = 4;
		final static int OPEN = 5;
		final static int CLOSE = 6;

		public PlayerCMD(int cmd) {
			this.cmd = cmd;
		}

		public PlayerCMD(int cmd, long stamp) {
			this.cmd = cmd;
			this.stamp = stamp;
		}

		public PlayerCMD(int cmd, String name) {
			this.cmd = cmd;
			this.name = name;
		}
	}

	/**
	 * Returns stream duration in ms
	 *
	 * @return
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Open a file.
	 * @param fileName
	 */
	public void open(String fileName) {
		if (getState() == Thread.State.NEW)
			start();

		cmdQueue.offer(new PlayerCMD(PlayerCMD.OPEN, fileName));
	}

	/**
	 * Close a file.
	 */
	public void close() {
		cmdQueue.offer(new PlayerCMD(PlayerCMD.CLOSE));
	}

	/**
	 * Seek to given time.
	 *
	 * This runs asynchronously and the video will have seeked at some following time.
	 *
	 * @param stamp timestamp in ms
	 * @param type, ignored
	 */
	public void seek(long stamp, int type) {
		cmdQueue.offer(new PlayerCMD(PlayerCMD.SEEK, stamp));
	}

	/**
	 * Pause at the current position.
	 */
	public void pause() {
		cmdQueue.offer(new PlayerCMD(PlayerCMD.PAUSE));
	}

	/**
	 * Resume at the current position.
	 */
	public void unpause() {
		cmdQueue.offer(new PlayerCMD(PlayerCMD.RESUME));
	}

	void setMediaState(MediaState state) {
		if (this.state != state) {
			this.state = state;
			if (listener != null)
				listener.mediaState(this, state);
		}
	}

	void mediaError(IOException ex) {
		if (listener != null)
			listener.mediaError(this, ex);
	}

	// FIXME: exceptions
	public void createDefaultDecoders(MediaSink dest) throws IOException {
		this.dest = dest;

		int nstreams = format.getNBStreams();
		AVStream vavstream = null;
		int vstream = -1;
		AVStream aavstream = null;
		int astream = -1;
		for (int i = 0; i < nstreams; i++) {
			AVStream s = format.getStreamAt(i);
			AVCodecContext cc = s.getCodec();
			int type = cc.getCodecType();
			if (vstream == -1 && type == AVCodecContext.AVMEDIA_TYPE_VIDEO) {
				vstream = i;
				vavstream = s;
			}
			if (astream == -1 && type == AVCodecContext.AVMEDIA_TYPE_AUDIO) {
				astream = i;
				aavstream = s;
			}
		}

		if (astream == -1 && vstream == -1) {
			throw new IOException("No media found");
		}

		duration = 0;
		if (vstream != -1) {
			VideoDecoder vd;
			streamMap.put(vstream, vd = new VideoDecoder(this, dest, vavstream, vstream));
			duration = vd.duration;
		}
		if (astream != -1) {
			AudioDecoder ad;
			streamMap.put(astream, ad = new AudioDecoder(this, dest, aavstream, astream));
			duration = Math.max(duration, ad.duration);
		}
	}

	@Override
	public void cancel() {
		System.out.println("Cancel: " + this);
		super.cancel();
		for (MediaDecoder dec : streamMap.values()) {
			dec.cancel();
		}
		streamMap.clear();
		if (format != null) {
			format.closeInput();
			format = null;
		}
	}
	int created;

	AVPacket createPacket() throws InterruptedException {
		if (created >= packetLimit) {
			return packetQueue.take();
		}

		AVPacket packet = packetQueue.poll();

		if (packet == null) {
			packet = AVPacket.create();
			created++;
			//System.out.println("creating new avpacket: " + created);
		}
		return packet;
	}

	public void recyclePacket(AVPacket packet) {
		packet.freePacket();
		packetQueue.offer(packet);
	}

	void postSeek() {
		for (MediaDecoder dec : streamMap.values()) {
			dec.postSeek();
		}
	}
	boolean initialised;

	void initDecoders() {
		// init all decoders if not already initialised
		if (!initialised) {
			for (MediaDecoder dec : streamMap.values()) {
				dec.init();
			}
			initialised = true;
		}
	}

	@Override
	public void run() {
		PlayerCMD cmd;

		// The IDLE and INIT states

		out:
		while (!cancelled) {
			try {
				cmd = cmdQueue.take();

				switch (state) {
					case Idle:
						/**
						 * IDLE state, allowed to quit or open a file.
						 */
						switch (cmd.cmd) {
							case PlayerCMD.QUIT:
								System.out.println("quit command");
								setMediaState(MediaState.Quit);
								break out;
							case PlayerCMD.OPEN:
								try {
									format = AVFormatContext.open(cmd.name);
									//format.setProbesize(1024 * 1024);
									//format.setMaxAnalyzeDuration(5000);
									format.findStreamInfo();
									setMediaState(MediaState.Init);
									initialised = false;
									file = cmd.name;
								} catch (IOException ex) {
									mediaError(ex);
								}
								break;
						}
						break;
					case Init:
					case Ready:
						/**
						 * INIT state, allows the caller to modify the decoded
						 * streams and so on.
						 * From this state one can seek or play or quit.
						 */
						switch (cmd.cmd) {
							case PlayerCMD.QUIT:
								System.out.println("quit command");
								setMediaState(MediaState.Quit);
								break out;
							case PlayerCMD.SEEK:
								if (state == MediaState.Init) {
									initDecoders();
									setMediaState(MediaState.Ready);
								}
								format.seekFile(-1, 0, cmd.stamp * 1000, cmd.stamp * 1000, 0);
								postSeek();
								dest.postSeek(cmd.stamp);
								break;
							case PlayerCMD.PLAY:
								if (state == MediaState.Init) {
									initDecoders();
									setMediaState(MediaState.Ready);
								}
								runMedia();
								break;
						}
						break;
					case Quit:
						break out;

				}
			} catch (InterruptedException ex) {
				if (cancelled)
					setMediaState(MediaState.Quit);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("media reader thread quit");
	}

	/**
	 * Above READY state - play the media, handle pause, etc.
	 */
	void runMedia() {
		AVPacket packet = null;
		boolean paused = false;

		setMediaState(MediaState.Playing);

		try {
			// ... if we get to the end of file this quits, but we don't want it to?
			packet = createPacket();

			out:
			while (!cancelled && format.readFrame(packet) >= 0) {
				try {
					// map to stream
					MediaDecoder md = streamMap.get(packet.getStreamIndex());
					if (md != null) {
						//System.out.println("sending packet to decoder " + packet.getStreamIndex() + " + " + packet.getSize());
						packet.dupPacket();
						md.enqueuePacket(packet);
						packet = null;
					}

					// check for control commands, collapse into a fixed set of commands.
					PlayerCMD seekcmd = null;
					PlayerCMD playcmd = null;
					PlayerCMD pausecmd = null;
					PlayerCMD cmd;

					// Keep waiting whilst paused
					do {
						// Collapse commands to fixed sequence
						do {
							if (paused)
								cmd = cmdQueue.take();
							else
								cmd = cmdQueue.poll();
							if (cmd != null) {
								switch (cmd.cmd) {
									case PlayerCMD.QUIT:
										System.out.println("quit command");
										setMediaState(MediaState.Quit);
										break out;
									case PlayerCMD.SEEK:
										System.out.println("seek command");
										seekcmd = cmd;
										break;
									case PlayerCMD.PAUSE:
										System.out.println("pause command");
										playcmd = null;
										pausecmd = cmd;
										break;
									case PlayerCMD.PLAY: // TODO: seek/re-open for play?
										System.out.println("play command");
										playcmd = cmd;
										pausecmd = null;
										break;
									case PlayerCMD.RESUME:
										System.out.println("resume command");
										playcmd = cmd;
										pausecmd = null;
										// Force exit from collapse loop
										cmd = null;
								}
							}
						} while (cmd != null);

						if (seekcmd != null) {
							format.seekFile(-1, 0, seekcmd.stamp * 1000, seekcmd.stamp * 1000, 0);
							postSeek();
							dest.postSeek(seekcmd.stamp);
						}
						if (pausecmd != null) {
							if (!paused) {
								System.out.println(" pause");
								dest.postPause();
								paused = true;
								setMediaState(MediaState.Paused);
							}
						} else if (playcmd != null) {
							if (paused) {
								System.out.println(" un-pause");
								paused = false;
								dest.postUnpause();
								setMediaState(MediaState.Playing);
							} else {
								dest.postPlay();
							}
						}
					} while (!cancelled && paused);
				} catch (InterruptedException x) {
					if (cancelled)
						setMediaState(MediaState.Quit);
				} finally {
					if (packet == null) {
						packet = createPacket();
					} else {
						packet.freePacket();
					}
				}
			}

			// Let decoders know it's end of file
			if (!cancelled) {
				System.out.println("Send final empty packet");
				for (MediaDecoder md : streamMap.values()) {
					packet = createPacket();
					packet.setData(null, 0);
					md.enqueuePacket(packet);
				}
				for (MediaDecoder md : streamMap.values()) {
					md.complete();
				}
			}
		} catch (InterruptedException x) {
			if (cancelled)
				setMediaState(MediaState.Quit);
		} catch (Exception x) {
			x.printStackTrace();
		} finally {
			System.out.println("File finished: " + file);

			// FIXME: handle shutdown somehow easier than this (e.g. 'postFinished'?)
			for (MediaDecoder dec : streamMap.values()) {
				dec.cancel();
			}

			streamMap.clear();
			format.closeInput();
			format = null;

			setMediaState(MediaState.Idle);
			dest.postFinished();
		}
	}
}
