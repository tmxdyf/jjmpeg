/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.notzed.jjmpeg.exception;

/**
 *
 * @author notzed
 */
public class AVIOException extends AVException {
	int errno;

	public AVIOException(int errno) {
	}

}
