package au.notzed.jjmpeg.exception;

/**
 *
 * @author notzed
 */
public class AVInvalidCodecException extends AVIOException {

	public int id;

	public AVInvalidCodecException(int id, String what) {
		super(what);
		this.id = id;
	}
}
