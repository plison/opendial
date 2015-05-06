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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.SpeechData;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.state.DialogueState;

import org.json.JSONArray;
import org.json.JSONObject;

import com.att.api.oauth.OAuthService;
import com.att.api.rest.APIResponse;
import com.att.api.rest.RESTClient;
import com.att.api.rest.RESTException;

/**
 * Plugin to access the AT&amp;T Speech API for both speech recognition and
 * synthesis. The plugin necessitates the specification of an application ID and
 * secret [see the README file for the plugin for details]. The API offers
 * cloud-based services for AT&amp;T's WATSON system.
 * 
 * <p>
 * To enhance the recognition accuracy, it is recommended to provide the system
 * with a recognition grammar, which can be specified in the GRXML format.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class ATTSpeech implements Module {

	// logger
	public static Logger log = new Logger("ATTSpeech", Logger.Level.DEBUG);

	/** Root of the URL for the API */
	public static final String FQDN = "https://api.att.com";

	/** dialogue state */
	DialogueSystem system;

	/** REST client for the speech recognition */
	RESTClient asrClient;

	/** grammar file */
	File grammarFile;

	/** Rest client for the speech synthesis */
	RESTClient ttsClient;

	/** whether the system is paused or active */
	boolean paused = true;

	/**
	 * Creates a new plugin, attached to the dialogue system
	 * 
	 * @param system the dialogue system to attach
	 * @throws DialException if the module could not be established
	 */
	public ATTSpeech(DialogueSystem system) throws DialException {
		this.system = system;
		List<String> missingParams = new LinkedList<String>(Arrays.asList(
				"key", "secret"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new DialException("Missing parameters: " + missingParams);
		}

		buildClients();

		if (system.getSettings().params.containsKey("grammar")) {
			File f = new File(
					system.getSettings().params.getProperty("grammar"));
			if (!f.exists()) {
				f = new File(
						(new File(system.getDomain().getName()).getParent()
								+ "/" + system.getSettings().params
								.getProperty("grammar")));
			}
			if (f.exists()) {
				grammarFile = f;
				log.info("AT&T Speech API will be used with the grammar: "
						+ grammarFile.getPath());
			} else {
				log.warning("Grammar file " + f.getPath() + " cannot be found");
				log.info("AT&T Speech API will be used without grammar");
			}
		} else {
			log.info("AT&T Speech API will be used without grammar");
		}

		system.enableSpeech(true);
	}

	/**
	 * Starts the AT&amp;T speech plugin.
	 */
	@Override
	public void start() throws DialException {
		paused = false;
		GUIFrame gui = system.getModule(GUIFrame.class);
		if (gui == null) {
			throw new DialException(
					"AT&T connection requires access to the GUI");
		}
	}

	/**
	 * Pauses the plugin.
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true if the plugin has been started and is not paused.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

	/**
	 * If the system output has been updated, trigger the speech synthesis.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		String speechVar = system.getSettings().userSpeech;
		String outputVar = system.getSettings().systemOutput;

		if (updatedVars.contains(speechVar) && state.hasChanceNode(speechVar)
				&& !paused) {
			Value speechVal = system.getContent(speechVar).getBest();
			if (speechVal instanceof SpeechData) {
				new Thread(
						() -> recognise((SpeechData) speechVal)).start();
			}
		} else if (updatedVars.contains(outputVar)
				&& state.hasChanceNode(outputVar) && !paused) {
			Value utteranceVal = system.getContent(outputVar).getBest();
			if (utteranceVal instanceof StringVal) {
				synthesise(utteranceVal.toString());
			}
		}
	}

	/**
	 * Processes the audio data contained in tempFile (based on the recognition
	 * grammar whenever provided) and returns the corresponding N-Best list of
	 * results.
	 * 
	 * @param stream the speech stream containing the audio data
	 * @param grammar the recognition grammar, which can be null
	 * @return the corresponding N-Best list of recognition hypotheses
	 */
	private void recognise(SpeechData stream) {

		log.info("calling AT&T server for recognition...\t");
		try {

			APIResponse apiResponse = asrClient.httpPost(grammarFile, stream,
					stream.getFormat());

			JSONObject object = new JSONObject(apiResponse.getResponseBodyStr());
			JSONObject recognition = object.getJSONObject("Recognition");
			final String jStatus = recognition.getString("Status");

			Map<String, Double> table = new HashMap<String, Double>();
			if (jStatus.equals("OK")) {
				JSONArray nBest = recognition.getJSONArray("NBest");
				for (int i = 0; i < nBest.length(); ++i) {
					JSONObject nBestObject = (JSONObject) nBest.get(i);
					if (nBestObject.has("Hypothesis")
							&& nBestObject.has("Confidence")) {
						String hyp = nBestObject.getString("Hypothesis");
						Double conf = nBestObject.getDouble("Confidence");
						table.put(hyp, conf);
					}
				}
			}

			if (!table.isEmpty()) {
				system.addUserInput(table);
			}
			
		} catch (Exception re) {
			re.printStackTrace();
		}
		
	}

	/**
	 * Performs remote speech synthesis with the given utterance, and plays it
	 * on the standard audio output.
	 * 
	 * @param utterance the utterance to synthesis
	 */
	public void synthesise(String utterance) {
		try {
			log.info("calling AT&T server for synthesis...\t");
			APIResponse apiResponse = ttsClient.httpPost(utterance);
			int statusCode = apiResponse.getStatusCode();
			if (statusCode == 200 || statusCode == 201) {
				SpeechData output = new SpeechData(apiResponse.getResponseBody());
				system.addContent(new Assignment(system.getSettings().systemSpeech, output));

			} else if (statusCode == 401) {
				throw new IOException("Unauthorized request.");
			} else {
				log.warning("TTS error: " + apiResponse);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the REST clients for speech recognition and synthesis.
	 * 
	 * @throws DialException
	 */
	private void buildClients() throws DialException {

		String key = system.getSettings().params.getProperty("key");
		String secret = system.getSettings().params.getProperty("secret");
		try {
			// Create service for interacting with the Speech api
			OAuthService osrvc = new OAuthService(FQDN, key, secret);
			asrClient = new RESTClient(FQDN + "/speech/v3/speechToTextCustom")
					.addAuthorizationHeader(osrvc.getToken("STTC"))
					.addHeader("Accept", "application/json")
					.addHeader("X-Arg", "HasMultipleNBest=true");

			ttsClient = new RESTClient(FQDN + "/speech/v3/textToSpeech")
					.addAuthorizationHeader(osrvc.getToken("TTS"))
					.addHeader("Content-Type", "text/plain")
					.addHeader("Accept", "audio/x-wav");

		} catch (RESTException e) {
			log.warning("Thrown exception: " + e);
			throw new DialException(
					"Cannot access AT&T API with the following credentials: "
							+ key + " --> " + secret);
		}
	}

}
