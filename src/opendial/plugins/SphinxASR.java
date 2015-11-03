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

import java.util.logging.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechData;
import opendial.modules.Module;
import opendial.utils.InferenceUtils;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.util.TimeFrame;

/**
 * Plugin for the CMU Sphinx 4 speech recogniser. Upon receiving a new speech stream
 * from the user (represented as a SpeechData object), the plugin performs the speech
 * recognition and adds the corresponding recognition results to the dialogue state.
 *
 * <p>
 * The plugin requires the specification of three parameters:
 * <ol>
 * <li>acousticmodel: the path to the directory containing the acoustic model
 * <li>dictionary: the path to the dictionary file
 * <li>grammar: the path to the grammar file in JSGF format, <b>OR</b>
 * <li>lm: the path to the statistical language model.
 * </ol>
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class SphinxASR implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Recognition probability for the best hypothesis returned by Sphinx. This quick
	 * and dirty hack is necessary at the moment since it seems difficult to retrieve
	 * scored N-Best lists from Sphinx when the language model is grammar-based.
	 */
	public static final double RECOG_PROB = 0.7;
	/**
	 * Maximum number of recognition results (only when using a language model)
	 */
	public static final int NBEST = 5;

	/** The dialogue system to which the ASR is connected */
	DialogueSystem system;

	/** the speech recogniser itself and its configuration */
	final Configuration configuration;
	StreamSpeechRecognizer asr;

	/** whether the ASR is active or paused */
	boolean isPaused = true;

	/**
	 * Creates the module for the Sphinx recognition engine, connected to the
	 * dialogue system.
	 * 
	 * <p>
	 * The path to the grammar file in JSGF format must be specified as parameter in
	 * the system settings (either by specifying it in the XML domain file or by
	 * adding "-Dgrammar=/path/grammar/file" to the command line).
	 * 
	 * @param system the dialogue system initialised
	 */
	public SphinxASR(DialogueSystem system) {

		this.system = system;
		Properties params = system.getSettings().params;

		configuration = new Configuration();

		// retrieving the path to the acoustic model
		if (params.containsKey("acousticmodel")) {
			String acousticModel = getFile(params.getProperty("acousticmodel"));
			configuration.setAcousticModelPath(acousticModel);
			log.info("Acoustic model: " + acousticModel);
		}
		else {
			throw new RuntimeException("Acoustic model must be provided");
		}

		// retrieving the path to the dictionary
		if (params.containsKey("dictionary")) {
			String dictionary = getFile(params.getProperty("dictionary"));
			configuration.setDictionaryPath(dictionary);
			log.info("Dictionary: " + dictionary);
		}
		else {
			throw new RuntimeException("Dictionary must be provided");
		}

		// retrieving the path to the grammar file
		if (params.containsKey("grammar")) {
			File grammarFile = new File(getFile(params.getProperty("grammar")));
			configuration.setGrammarPath(grammarFile.getParent() + File.separator);
			configuration.setGrammarName(grammarFile.getName().replace(".gram", ""));
			configuration.setUseGrammar(true);
			log.info("Recognition grammar: " + grammarFile.getPath());
		}
		// else, retrieving the path to the statistical language model
		else if (params.containsKey("lm")) {
			String slm = getFile(params.getProperty("lm"));
			configuration.setLanguageModelPath(slm);
			configuration.setUseGrammar(false);
			log.info("Statistical language model: " + slm);
		}

		else {
			throw new RuntimeException(
					"Must provide either grammar or language model");
		}

		System.getProperties().setProperty("logLevel", "OFF");
		try {
			asr = new StreamSpeechRecognizer(configuration);
		}
		catch (IOException e) {
			throw new RuntimeException("cannot start Sphinx recognizer: " + e);
		}
		system.enableSpeech(true);
	}

	/**
	 * Starts the recogniser.
	 */
	@Override
	public void start() {
		isPaused = false;
	}

	/**
	 * Performs the speech recognition upon receiving a new user speech stream.
	 * Otherwise, does nothing.
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the set of updated variables
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		String speechVar = system.getSettings().userSpeech;

		if (updatedVars.contains(speechVar) && state.hasChanceNode(speechVar)
				&& !isPaused) {
			Value speechVal = system.getContent(speechVar).toDiscrete().getBest();
			if (speechVal instanceof SpeechData) {
				(new Thread(new RecognitionProcess((SpeechData) speechVal))).start();
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
	 * Returns true if the module is currently running (= if it has been started and
	 * is not paused).
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
	private Map<String, Double> createNBestList(SpeechResult result) {
		Map<String, Double> table = new HashMap<String, Double>();
		if (configuration.getUseGrammar()) {
			String hypothesis = result.getHypothesis().trim();
			if (hypothesis.length() > 0 && !hypothesis.equals("<unk>")) {
				table.put(hypothesis, RECOG_PROB);
			}
		}
		else {
			for (String r : result.getNbest(NBEST)) {
				String hypothesis = r.replaceAll("</?s>", "").trim();
				if (hypothesis.length() > 0 && !hypothesis.equals("<unk>")) {
					// since getting explicit scores from Sphinx is difficult, we
					// simply calculate a score from the position of the hypothesis
					// in the N-Best list (an hypothesis at position i gets a score
					// that is half the one of the hypothesis at i-1).
					table.put(hypothesis, 1.0 / (table.size() + 1));
				}
			}
			table = InferenceUtils.normalise(table);
		}
		return table;
	}

	/**
	 * Retrieves the file or resource associated with the given path
	 * 
	 * @param path the path
	 * @return the corresponding file object
	 */
	private String getFile(String path) {
		if (path.startsWith("resource:")) {
			String resource =
					path.replace("resource:/", "").replace("resource:", "");
			URL u = SphinxASR.class.getClassLoader().getResource(resource);
			if (u == null) {
				throw new RuntimeException(
						"Resource " + resource + " cannot be found");
			}
			return "resource:/" + resource;

		}
		File f = new File(path);
		if (!f.exists() && !system.getDomain().isEmpty()) {
			String rootpath =
					system.getDomain().getSourceFile().getParent() + File.separator;
			f = new File(rootpath + path);
		}
		if (!f.exists()) {
			throw new RuntimeException("File " + f + " cannot be found");
		}
		return f.getAbsolutePath();
	}

	/**
	 * Thread for a speech recognition process
	 */
	class RecognitionProcess implements Runnable {

		SpeechData stream;

		public RecognitionProcess(SpeechData stream) {
			this.stream = stream;
		}

		@Override
		public void run() {
			try {
				log.fine("start Sphinx recognition...");
				asr.startRecognition(stream, new TimeFrame(15000));
				SpeechResult curResult = asr.getResult();
				log.info((curResult == null) ? "No recognition results "
						: "Recognition completed");
				if (curResult != null) {
					Map<String, Double> results = createNBestList(curResult);
					system.addUserInput(results);
				}
				asr.stopRecognition();
			}
			catch (Exception e) {
				log.warning("cannot do recognition: " + e.toString());
				try {
					asr = new StreamSpeechRecognizer(configuration);
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
