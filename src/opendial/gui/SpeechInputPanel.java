// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.modules.core.AudioModule;

/**
 * Panel employed to capture audio input through a press and hold button,
 * accompanied by a sound level meter. The captured sound is then sent to the
 * dialogue system for further processing by the speech recognition engine.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
@SuppressWarnings("serial")
public class SpeechInputPanel extends JPanel implements MouseListener {

	// logger
	public static Logger log = new Logger("AudioPanel", Logger.Level.DEBUG);

	// the audio recorder
	AudioModule recorder;

	// the current volume
	int volume = 0;

	// sound level meter
	SoundLevelMeter slm;

	/**
	 * Creates the speech input panel, composed of a press and hold button and a
	 * sound level meter.
	 * 
	 * @param system the dialogue system (to which the stream is being
	 *            forwarded)
	 */
	public SpeechInputPanel(AudioModule recorder) {
		this.recorder = recorder;
		Container container = new Container();
		container.setLayout(new BorderLayout());
		JButton button = new JButton(
				"<html>Press & hold to record speech</html>");
		button.addMouseListener(this);
		container.add(new JLabel("<html><b>Audio capture</b>:&nbsp;"),
				BorderLayout.WEST);
		container.add(button, BorderLayout.CENTER);
		Container volumeCont = new Container();
		volumeCont.setLayout(new BorderLayout());
		volumeCont
				.add(new JLabel(
						"<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Volume:&nbsp;</html>"),
						BorderLayout.CENTER);
		slm = new SoundLevelMeter();
		slm.setPreferredSize(new Dimension(200, 20));
		slm.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		volumeCont.add(slm, BorderLayout.EAST);
		container.add(volumeCont, BorderLayout.EAST);
		final JCheckBox checkbox = new JCheckBox("Voice Activity Detection");
		checkbox.addActionListener(a -> {
			recorder.activateVAD(checkbox.isSelected());
			button.setEnabled(!checkbox.isSelected()); 
		});
		container.add(checkbox, BorderLayout.SOUTH);
		add(container);
		
		Thread t = new Thread(() -> {
			while (true) {			
				slm.updateVolume(recorder.getVolume());
				try {Thread.sleep(100);	} 
				catch (InterruptedException e) {
				}
			}
		});
		t.start();
	}

	/**
	 * Starts the recording
	 *
	 * @param e mouse event (ignored)
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		try {
			recorder.startRecording();
		} catch (DialException ex) {
			log.warning(ex.toString());
		}
	}

	/**
	 * Stops the recording, and trigger the dialogue system if it is above the
	 * minimum recording time.
	 *
	 * @param e mouse event (ignored)
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		try {
			recorder.stopRecording();
		} catch (Exception f) {
			f.printStackTrace();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	
	class SoundLevelMeter extends JPanel {
		
		/**
		 * Updates the volume on the meter
		 * 
		 * @param vol the new volume
		 */
		private void updateVolume(double vol) {
			if (volume != vol) {
			volume = (int) vol;
			repaint();
			}
		}

		/**
		 * Repaint
		 *
		 * @param gg the graphics
		 */
		@Override
		public void paintComponent(Graphics gg) {
			gg.setColor(Color.GREEN);
			gg.clearRect(0, 0, 150, 20);
			gg.fillRect(0, 0, volume/20, 20);
		}


	}

}
