/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.notzed.jjmpeg;

/**
 *
 * @author notzed
 */
public class AVDictionary extends AVDictionaryAbstract {

	public AVDictionary(int p) {
		setNative(new AVDictionaryNative32(this, p));
	}

	public AVDictionary(long p) {
		setNative(new AVDictionaryNative64(this, p));
	}
}

class AVDictionaryNative extends AVDictionaryNativeAbstract {

	public AVDictionaryNative(AVObject o) {
		super(o);
	}
}

class AVDictionaryNative32 extends AVDictionaryNative {

	int p;

	AVDictionaryNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}
}

class AVDictionaryNative64 extends AVDictionaryNative {

	long p;

	AVDictionaryNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}
}
