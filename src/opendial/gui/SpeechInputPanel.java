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

import java.util.logging.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import opendial.modules.AudioModule;

/**
 * Panel employed to capture audio input through a press and hold button, accompanied
 * by a sound level meter. The captured sound is then sent to the dialogue system for
 * further processing by the speech recognition engine.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
@SuppressWarnings("serial")
public class SpeechInputPanel extends JPanel implements MouseListener {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the audio recorder
	AudioModule recorder;

	// the current volume
	int volume;

	// the sound level meter;
	JProgressBar slm;

	/**
	 * Creates the speech input panel, composed of a press and hold button and a
	 * sound level meter.
	 * 
	 * @param recorder the audiomodule the audiomodule associated with the panel
	 */
	public SpeechInputPanel(AudioModule recorder) {
		this.recorder = recorder;
		recorder.attachPanel(this);
		setLayout(new BorderLayout());

		final JCheckBox checkbox = new JCheckBox("Voice Activity Detection");
		add(checkbox, BorderLayout.LINE_START);
		Container container = new Container();
		container.setLayout(new FlowLayout());
		container.add(new JLabel(""));
		JButton button = new JButton(
				"<html>&nbsp;&nbsp;Press & hold to record speech&nbsp;&nbsp;&nbsp;&nbsp;</html>");
		button.addMouseListener(this);
		container.add(button);
		container.add(new JLabel(""));
		add(container);
		slm = new JProgressBar();
		slm.setMaximum(2000);
		slm.setBorderPainted(true);
		slm.setString("System is talking...");
		slm.setPreferredSize(new Dimension(200, 25));
		add(slm, BorderLayout.LINE_END);
		this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		checkbox.addActionListener(a -> {
			recorder.activateVAD(checkbox.isSelected());
			button.setEnabled(!checkbox.isSelected());
		});

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
		}
		catch (RuntimeException ex) {
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
		}
		catch (Exception f) {
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

	/**
	 * Updates the volume value in the meter.
	 * 
	 * @param currentVolume the new volume
	 */
	public void updateVolume(int currentVolume) {
		if (Math.abs(currentVolume - volume) > 20) {
			volume = currentVolume;
			slm.setValue(volume);
		}
	}

	/**
	 * Clears the volume in the meter
	 */
	public void clearVolume() {
		if (volume != 0) {
			volume = 0;
			slm.setValue(0);
		}
	}

	/**
	 * Sets a string in the volume meter indicating that the system is currently
	 * talking.
	 * 
	 * @param systemTalks whether the system currently talks or not
	 */
	public void setSystemTalking(boolean systemTalks) {
		slm.setStringPainted(systemTalks);
	}

}
