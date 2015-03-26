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

package opendial.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechStream;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.state.DialogueState;


/**
 * Plugin for the CMU Sphinx 4 speech recogniser. Upon recording a new speech stream,
 * the plugin sends the audio data to the recogniser, gets back the recognition 
 * results and adds the corresponding input to the dialogue state.
 * 
 * <p>The plugin requires the specification of a recognition grammar in JSGF format, but can
 * be easily adapted to instead employ a statistical language model. The plugin uses a 
 * wideband acoustic model trained on the Wall Street Journal (dictation domain, with microphone
 * speech) and the CMU pronunciation. dictionary. These models can also be straightforwardly
 * changed, depending on the particular needs of the application.
 * 
 *  
 * @author  Pierre Lison (plison@ifi.uio.no)
 */
public class SphinxASR implements Module {

	// logger
	public static Logger log = new Logger("SphinxASR", Logger.Level.DEBUG);

	/** Acoustic model */
	public static final String ACOUSTIC_MODEL = "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz";
	
	/** Pronunciation dictionary */
	public static final String DICTIONARY = "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/dict/cmudict.0.6d";
	
	/** Recognition probability for the best hypothesis returned by Sphinx. This 
	 * quick and dirty hack is necessary at the moment since it seems difficult
	 * to retrieve scored N-Best lists from Sphinx when the language model is
	 * grammar-based.
	 */
	public static final double RECOG_PROB = 0.7;
	
	/** The dialogue system to which the ASR is connected */
	DialogueSystem system;
	
	/** the speech recogniser itself */
	StreamSpeechRecognizer asr;

	/** recognition grammar (in JSGF format) */
	File grammarFile;
	
	/** whether the ASR is active or paused */
	boolean isPaused = true;
 
	
	/**
	 * Creates the module for the Sphinx recognition engine, connected to the dialogue
	 * system. The path to the recognition grammar file (in JSGF format) must be specified 
	 * as parameter in the system settings.
	 * 
	 * @param system the dialogue system
	 * @throws DialException if the recognition could not be initialised
	 */
	public SphinxASR(DialogueSystem system) throws DialException {
		
		this.system = system;
		
		if (system.getSettings().params.containsKey("grammar")) {
			File f = new File(system.getSettings().params.getProperty("grammar"));
			if (!f.exists()) {
				f = new File((new File(system.getDomain().getName()).getParent() +
						"/" + system.getSettings().params.getProperty("grammar")));
			}
			if (f.exists()) {
				grammarFile = f;
				log.info("Sphinx ASR will be used with the grammar: " + grammarFile.getPath());
			}
			else {
				throw new DialException("Grammar file " + f.getPath() + " cannot be found");						
			}
		}
		else {
			throw new DialException("Grammar file must be specified");		
		}
		
		Configuration configuration = new Configuration();
		configuration.setAcousticModelPath(ACOUSTIC_MODEL);
		configuration.setDictionaryPath(DICTIONARY);
		configuration.setGrammarPath(grammarFile.getParent()+"/");
		configuration.setGrammarName(grammarFile.getName().replace(".gram", ""));	
		configuration.setUseGrammar(true);
		try {
			asr = new StreamSpeechRecognizer(configuration);
		} catch (IOException e) {
			throw new DialException("cannot start Sphinx recognizer: " + e);
		}
		
		if (system.getModule(GUIFrame.class) != null) {
			system.getModule(GUIFrame.class).enableSpeech(true);
		}
	}

	/**
	 * Starts the recogniser.
	 */
	@Override
	public void start() throws DialException {
		isPaused = false;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		String speechVar = system.getSettings().userSpeech;
		
		if (updatedVars.contains(speechVar) && state.hasChanceNode(speechVar) && !isPaused) {
			Value speechVal = system.getContent(speechVar).toDiscrete().getBest();
			if (speechVal instanceof SpeechStream) {
				(new Thread(new RecognitionProcess((SpeechStream)speechVal))).start();
			}
		}
	}

	/**
	 * Pauses or unpauses the recogniser.
	 * 
	 * @param toPause true if the system should be paused, false otherwise
	 */
	@Override
	public void pause(boolean toPause) {
		isPaused = toPause;
	}

	/**
	 * Returns true if the module is currently running (= if it has been started
	 * and is not paused).
	 * 
	 * @return true if the module is running, false otherwise
	 */
	@Override
	public boolean isRunning() {
		return !isPaused;
	}

	
	
	/**
	 * Creates the N-best list.
	 * 
	 * @param result the speech result from Sphinx
	 * @return the corresponding N-best list
	 */
	private CategoricalTable createNBestList(SpeechResult result) {
		CategoricalTable table = new CategoricalTable(system.getSettings().userInput);
		table.addRow(result.getHypothesis(), RECOG_PROB);
		return table;
	}
	
	


	/**
	 * Thread for a speech recognition process
	 */
	class RecognitionProcess implements Runnable {

		SpeechStream stream;
		
		public RecognitionProcess(SpeechStream stream) {
			this.stream = stream;
		}

		@Override
		public void run() {
			try {
					log.debug("start Sphinx recognition...");
					asr.startRecognition(stream);
					SpeechResult curResult = asr.getResult();
					if (curResult != null) {
					CategoricalTable results = createNBestList(curResult);
					system.addContent(results);
					}
					asr.stopRecognition();
					
			}
			catch (Exception e) {
				e.printStackTrace();
				log.warning("cannot do recognition: " + e);
			}
			}
	}
}
