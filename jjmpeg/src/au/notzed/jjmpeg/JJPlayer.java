package au.notzed.jjmpeg;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import au.notzed.jjmpeg.exception.AVIOException;
import au.notzed.jjmpeg.exception.AVInvalidCodecException;
import au.notzed.jjmpeg.exception.AVInvalidStreamException;
import au.notzed.jjmpeg.io.JJMediaReader;
import au.notzed.jjmpeg.io.JJMediaReader.JJReaderVideo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Totally dunk demo that just opens a file, reads some frames
 * and displays the bitmap.
 * @author notzed
 */
public class JJPlayer extends Activity {
	ImageView iview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			AVFormatContext.registerAll();
			try {
				AVFormatContext fmt = AVFormatContext.open("/sdcard/trailer.mp4");

				fmt.closeInput();
			} catch (AVIOException ex) {
				Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
			}
		} catch (Throwable t) {
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, t);
		}

		JJMediaReader mr = null;
		try {
			//mr = new JJMediaReader("/sdcard/bbb.mov");
			mr = new JJMediaReader("/sdcard/trailer.mp4");
			JJReaderVideo vs = mr.openFirstVideoStream();

			Log.i("jjplayer", String.format("Opened Video: %dx%d fmt %s", vs.getWidth(), vs.getHeight(), vs.getPixelFormat()));

			int w = vs.getWidth();
			int h = vs.getHeight();

			Bitmap bm = Bitmap.createBitmap(w, h, Config.ARGB_8888);

			PixelFormat fmt = PixelFormat.PIX_FMT_RGBA;
			vs.setOutputFormat(fmt, w, h);

			int fc = 0;
			int n = 10*25;
			long now = System.currentTimeMillis();
			// jump a few frames in to get past titles
			for (int i=0;i<n;i++) {
				mr.readFrame();
				fc++;
			}
			now = System.currentTimeMillis() - now;
			Log.i("jjplayer", String.format("Decoding %d frames took %d.%03ds", fc, now/1000, now%1000));

			// extract frame
			AVPlane plane = vs.getOutputFrame().getPlaneAt(0, fmt, w, h);
			bm.copyPixelsFromBuffer(plane.data);


			iview = (ImageView) findViewById(R.id.image);
			iview.setImageBitmap(bm);

			mr.dispose();
			mr = null;
		} catch (AVInvalidStreamException ex) {
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (AVIOException ex) {
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (AVInvalidCodecException ex) {
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (mr != null)
				mr.dispose();
		}

		File f = new File("/sdcard/trailer.mp4");
		try {
			FileInputStream fis = new FileInputStream(f);

			Log.i("jjplayer", "Opened file ok size=" + fis.getChannel().size());

			fis.close();
		} catch (IOException ex) {
			Log.e("jjplayer", "Opened file failed " + ex);
			Logger.getLogger(JJPlayer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
