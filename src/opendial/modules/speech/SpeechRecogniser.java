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

package opendial.modules.speech;


import opendial.datastructs.SpeechStream;
import opendial.modules.Module;

/**
 * Basic interface for a speech recogniser that takes an audio
 * stream as input and updates the dialogue state with its
 * recognition results.  Speech recognisers are allowed to
 * to operate incrementally and update the dialogue state as
 * the utterance unfolds.
 * 
 * <p>Each implementation of SpeechRecogniser must also implement 
 * the methods specified in the Module interface.
 * 
 * <p>The audio mixer to use for the sound capture is specified in
 * Settings.inputMixer.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public interface SpeechRecogniser extends Module {
	
	/**
	 * Processes the audio stream and update the dialogue state with 
	 * the corresponding results from the recognition process.
	 * 
	 * @param audioStream the audio stream
	 */
	public void processInput (SpeechStream audioStream) ;
	
}

