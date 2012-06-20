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

/**
 * Interface to a player that takes the audio/video frames and synchronises them.
 *
 * @author notzed
 */
public interface MediaSink {

	public void addMediaSinkListener(MediaSinkListener listener);

	public AudioFrame getAudioFrame() throws InterruptedException;

	public VideoFrame getVideoFrame() throws InterruptedException;

	public void postSeek(long stampms);

	public void pause();

	public void unpause();

	public void finished();
}
