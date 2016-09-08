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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechData;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.utils.InferenceUtils;
import opendial.utils.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Plugin to access the Nuance Speech API for both speech recognition and synthesis.
 * The plugin necessitates the specification of an application ID and key [see the
 * README file for the plugin for details]. The API offers a cloud-based access to
 * the Nuance Mobile developer platform.
 * 
 * <p>
 * To enhance the recognition accuracy, it is recommended to provide the system with
 * custom vocabularies (see the documentation on the Nuance website).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class WatsonSpeech implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** dialogue state */
	DialogueSystem system;

	/** HTTP client and URI for the speech recognition */
	CloseableHttpClient asrClient;
	URI asrURI;

	/** HTTP client and URI for the speech synthesis */
	CloseableHttpClient ttsClient;
	URI ttsURI;

	/** Cache of previously synthesised system utterances */
	Map<String, SpeechData> ttsCache;

	/** List of speech outputs currently being synthesised */
	List<SpeechData> currentSynthesis;

	/** whether the system is paused or active */
	boolean paused = true;
	
	

	/**
	 * Creates a new plugin, attached to the dialogue system
	 * 
	 * @param system the dialogue system to attach @ in case of missing parameters
	 */
	public WatsonSpeech(DialogueSystem system) {
		this.system = system;
		/*List<String> missingParams =
				new LinkedList<String>(Arrays.asList("id", "key", "lang"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}*/

		currentSynthesis = new ArrayList<SpeechData>();
		buildClients();

		system.enableSpeech(true);
	}

	/**
	 * Starts the Nuance speech plugin.
	 */
	@Override
	public void start() {
		paused = false;
		GUIFrame gui = system.getModule(GUIFrame.class);
		if (gui == null) {
			throw new RuntimeException(
					"Watson connection requires access to the GUI");
		}
		ttsCache = new HashMap<String, SpeechData>();
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

		String userSpeechVar = system.getSettings().userSpeech;
		String outputVar = system.getSettings().systemOutput;

		// if a new user speech is detected, start the speech recognition
		if (updatedVars.contains(userSpeechVar) && state.hasChanceNode(userSpeechVar)
				&& !paused) {

			Value speechVal = system.getContent(userSpeechVar).getBest();
			if (speechVal instanceof SpeechData) {
				Thread t = new Thread(() -> recognise((SpeechData) speechVal));
				t.start();

			}
		}

		// if a new system speech is detected, start speech synthesis
		else if (updatedVars.contains(outputVar) && state.hasChanceNode(outputVar)
				&& !paused) {
			Value utteranceVal = system.getContent(outputVar).getBest();
			if (utteranceVal instanceof StringVal) {
				synthesise(utteranceVal.toString());
			}
		}
	}

	/**
	 * Processes the audio data contained in tempFile (based on the recognition
	 * grammar whenever provided) and updates the dialogue state with the new user
	 * inputs.
	 * 
	 * @param stream the speech stream containing the audio data
	 */
	private void recognise(SpeechData stream) {

		int sampleRate = (int) stream.getFormat().getSampleRate();
		log.fine("calling Watson server for recognition... " + "(sample rate: "
				+ sampleRate + " Hz.)");
		try {

			HttpPost httppost = new HttpPost(asrURI);
			String format = "audio/x-wav;codec=pcm;bit="
					+ stream.getFormat().getFrameSize() * 8 + ";rate=" + sampleRate;
			//String lang = system.getSettings().params.getProperty("lang");
			System.out.println("WatsonSpeech format=" + format);
			httppost.addHeader("Content-Type", "audio/raw;rate=16000;coding=linear;byteorder=LE");
			httppost.addHeader("Accept", "json");
			httppost.addHeader("X-Arg", "AccessKey=123");
			//httppost.addHeader("Accept-Language", lang);
			//httppost.addHeader("Content-Language", lang);
			//httppost.addHeader("Accept-Topic", "Dictation");

			InputStreamEntity reqEntity = new InputStreamEntity(stream);
			reqEntity.setContentType("audio/wav");
			httppost.setEntity(reqEntity);
			System.out.println(stream.getData());

			HttpResponse response = asrClient.execute(httppost);
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				log.warning("Response entity is null, aborting");
			} 
			
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(resEntity.getContent()));

			if (response.getStatusLine().getStatusCode() != 200) {
				log.warning("(speech could not be recognised: error "
						+ response.getStatusLine().getStatusCode() + ")");
				String sentence;
				while ((sentence = reader.readLine()) != null) {
					log.warning(sentence);
				}
			}
			else {

				String resultStr = "";
				String line;
				//the result is a single json string 
				//read it in
				while ((line = reader.readLine()) != null) {
					resultStr += line; 
				}
				
				//put each hypothesis into hash
				Map<String, Double> hypotheses = new HashMap<String, Double>();
				JSONObject jsonResult = new JSONObject(resultStr);
				JSONArray nbest = (JSONArray) ((JSONObject) jsonResult.get("Recognition")).get("NBest");
				log.info("JsonArray=" + nbest.toString());
				for (int i = 0; i < nbest.length(); i++)
				{
					String hyp = ((JSONObject) nbest.get(i)).getString("ResultText");
					hypotheses.put(hyp,  1.0 / (hypotheses.size() + 1));
				}
				hypotheses = InferenceUtils.normalise(hypotheses);
				
				for (String s : new ArrayList<String>(hypotheses.keySet())) {
					hypotheses.put(s, ((int) (hypotheses.get(s) * 100)) / 100.0);
				}


				log.info("recognition results: " + hypotheses);
				reader.close();
				if (!hypotheses.isEmpty()) {
					system.addUserInput(hypotheses);
				}
			}
			httppost.releaseConnection();
		}
		catch (Exception e) {
			log.warning("could not extract ASR results: " + e);
		}
	}

	/**
	 * Synthesises the provided utterance (first looking at the cache of existing
	 * synthesised speech, and starting the generation if no one is already present).
	 * 
	 * @param utterance the utterance to synthesise
	 */
	private void synthesise(String utterance) {

		String systemSpeechVar = system.getSettings().systemSpeech;

		SpeechData outputSpeech;
		if (ttsCache.containsKey(utterance)) {
			outputSpeech = ttsCache.get(utterance);
			outputSpeech.rewind();
		}
		else {
			AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
			outputSpeech = new SpeechData(format);
			new Thread(() -> synthesise(utterance, outputSpeech)).start();
		}

		currentSynthesis.add(outputSpeech);
		new Thread(() -> {
			while (!currentSynthesis.get(0).equals(outputSpeech)) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
			}
			system.addContent(systemSpeechVar, outputSpeech);
			currentSynthesis.remove(0);
		}).start();
	}

	/**
	 * Synthesises the provided utterance and adds the resulting stream of audio data
	 * to the SpeechData object.
	 * 
	 * @param utterance the utterance to synthesise
	 * @param output the speech data in which to write the generated audio
	 */
	private void synthesise(String utterance, SpeechData output) {

		try {
			log.info("calling Nuance server to synthesise utterance \"" + utterance
					+ "\"");

			HttpPost httppost = new HttpPost(ttsURI);
			httppost.addHeader("Content-Type", "text/plain");
			httppost.addHeader("Accept", "audio/x-wav;codec=pcm;bit=16;rate=16000");
			HttpEntity entity = new StringEntity(utterance, "utf-8");
			;
			httppost.setEntity(entity);

			HttpResponse response = ttsClient.execute(httppost);

			HttpEntity resEntity = response.getEntity();
			if (resEntity == null
					|| response.getStatusLine().getStatusCode() != 200) {
				log.info("Response status: " + response.getStatusLine());
				return;
			}
			output.write(resEntity.getContent());
			httppost.releaseConnection();
			output.setAsFinal();
			ttsCache.put(utterance, output);
			log.info("... Speech synthesis completed (speech duration: "
					+ StringUtils.getShortForm((double) output.length() / 1000)
					+ " s.)");

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the REST clients for speech recognition and synthesis.
	 * Use Watson for ASR and Nuance for TTS
	 * @
	 */
	private void buildClients() {

		// Initialize the HTTP clients
		asrClient = HttpClientBuilder.create().build();
		ttsClient = HttpClientBuilder.create().build();

		try {

			URIBuilder builderAsr = new URIBuilder();
			builderAsr.setScheme("http");
			builderAsr.setHost("scorpion-5.som.ma");
			builderAsr.setPort(8882);
			builderAsr.setPath("/sveta/asr");
			asrURI = builderAsr.build();

			URIBuilder builderTts = new URIBuilder();
			builderTts.setScheme("https");
			builderTts.setParameter("appId",
					system.getSettings().params.getProperty("id"));
			builderTts.setParameter("appKey",
					system.getSettings().params.getProperty("key"));
			builderTts.setParameter("id", "0000"); 
			builderTts.setPort(443);
			builderTts.setHost("tts.nuancemobility.net");
			builderTts.setPath("/NMDPTTSCmdServlet/tts");
			builderTts.setParameter("ttsLang",
					system.getSettings().params.getProperty("lang"));
			ttsURI = builderTts.build();

		}
		catch (Exception e) {
			throw new RuntimeException("cannot build client: " + e);
		}
	}

}
