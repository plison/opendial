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

package oblig2.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import oblig2.DialogueSystem;
import oblig2.util.AudioRecorder;
import oblig2.util.Logger;

/**
 * Listener object attached to the push & speak button.  The object starts the recording
 * when the mouse button is pressed, and stops the recording when it is released.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RecorderListener implements MouseListener {

	// logger
	public static Logger log = new Logger("RecorderListener", Logger.Level.NORMAL);
	
	// dialogue system owner
	DialogueSystem owner; 
	
	// the audio recorder itself
	AudioRecorder recorder;

	// time since the button was pressed
	long startTime = 0;

	/**
	 * Creates a new recorder listener, with an audio recorder and a dialogue
	 * state
	 * 
	 * @param recorder the audio recorder
	 * @param dstate the dialogue state
	 */
	public RecorderListener(DialogueSystem owner) {
		this.recorder = new AudioRecorder(owner.getParameters());
		this.owner = owner;
	}
	
	
	/**
	 * Starts the recording 
	 *
	 * @param e mouse event (ignored)
	 */
	public void mousePressed(MouseEvent e) { 
		startTime = System.currentTimeMillis() ;
		recorder.startRecording();
	}

	/**
	 * Stops the recording, and trigger the dialogue system if it is above
	 * the minimum recording time.
	 *
	 * @param e mouse event (ignored)
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		recorder.stopRecording();  
		if (System.currentTimeMillis() - startTime > owner.getParameters().minimumRecordingTime) {
			owner.getDialogueState().newSpeechSignal(new File(owner.getParameters().tempASRSoundFile));
		}
		else {
			log.info("recording is discarded: too short duration ( < 1s)");
		}
	}
	
	
	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	
}
