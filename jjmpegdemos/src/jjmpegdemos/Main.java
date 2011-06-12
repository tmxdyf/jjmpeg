/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jjmpegdemos;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author notzed
 */
public class Main {

	static File chooseFile() {
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(null) == fc.APPROVE_OPTION) {
			return fc.getSelectedFile();
		} else {
			return null;
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				JFrame win = new JFrame("JJMPEG Demos");
				JPanel jp = new JPanel();

				jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));

				win.add(jp);

				jp.add(new JButton(new AbstractAction("Audio Player") {

					public void actionPerformed(ActionEvent e) {
						AudioPlayer.main(new String[0]);
					}
				}));

				win.pack();
				win.setDefaultCloseOperation(win.EXIT_ON_CLOSE);
				win.setVisible(true);
			}
		});
	}
}
