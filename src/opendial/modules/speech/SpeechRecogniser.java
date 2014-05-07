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

