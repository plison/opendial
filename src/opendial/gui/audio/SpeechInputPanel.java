// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.gui.audio;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.SpeechStream;


/**
 * Panel employed to capture audio input through a press and hold button,
 * accompanied by a sound level meter.  The captured sound is then sent to the
 * dialogue system for further processing by the speech recognition engine.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
@SuppressWarnings("serial")
public class SpeechInputPanel extends JPanel implements MouseListener {

	// logger
	public static Logger log = new Logger("AudioPanel", Logger.Level.DEBUG);

	// speech recogniser
	DialogueSystem system;
	
	// current speech recording
	SpeechStream currentRecording;
	
	// sound level meter
	SoundLevelMeter slm;

	/**
	 * Creates the speech input panel, composed of a press and hold button and a sound level 
	 * meter.
	 * 
	 * @param system the dialogue system (to which the stream is being forwarded)
	 */
	public SpeechInputPanel(DialogueSystem system) {
		this.system = system;
		Container container = new Container();
		container.setLayout(new BorderLayout());
		JButton  button = new JButton("<html>Press & hold to record speech</html>");
		button.addMouseListener(this);
		container.add(new JLabel("<html><b>Audio capture</b>:&nbsp;"), BorderLayout.WEST);
		container.add(button, BorderLayout.CENTER);
		Container volumeCont = new Container();
		volumeCont.setLayout(new BorderLayout());
		volumeCont.add(new JLabel("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Volume:&nbsp;</html>"), BorderLayout.CENTER); 		
		slm = new SoundLevelMeter();
		slm.setPreferredSize(new Dimension(200, 20));
		slm.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		volumeCont.add(slm, BorderLayout.EAST);
		container.add(volumeCont, BorderLayout.EAST);
		add(container);
	}


	/**
	 * Starts the recording 
	 *
	 * @param e mouse event (ignored)
	 */
	@Override
	public void mousePressed(MouseEvent e) { 
		try {
		currentRecording = new SpeechStream(system.getSettings().inputMixer);
		slm.monitorVolume(currentRecording);
		system.addContent(currentRecording);
		}
		catch (DialException ex) {
			log.warning(ex.toString());
		}
	}

	/**
	 * Stops the recording, and trigger the dialogue system if it is above
	 * the minimum recording time.
	 *
	 * @param e mouse event (ignored)
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		try {
		currentRecording.close(); 
		}
		catch (Exception f) {
			f.printStackTrace();
		}
	}
	
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
}

