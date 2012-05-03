/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.notzed.jjmpeg;


/**
 *
 * @author notzed
 */
public class AVDictionary extends AVDictionaryAbstract{

	public AVDictionary(int p) {
		setNative(new AVDictionaryNative(this, p));
	}

}
class AVDictionaryNative extends AVDictionaryNativeAbstract {
	int p;

	public AVDictionaryNative(AVObject o, int p) {
		super(o);

		this.p = p;
	}
}
