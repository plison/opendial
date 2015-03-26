
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.SpeechStream;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.utils.AudioUtils;
import opendial.utils.InferenceUtils;

/**
 * Plugin to access the Nuance Speech API for both speech recognition and synthesis.
 * The plugin necessitates the specification of an application ID and key
 * [see the README file for the plugin for details].  The API offers
 * a cloud-based access to the Nuance Mobile developer platform.
 * 
 * <p>To enhance the recognition accuracy, it is recommended to provide the system
 * with custom vocabularies (see the documentation on the Nuance website).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 */
public class NuanceSpeech implements Module {

	// logger
	public static Logger log = new Logger("NuanceSpeech", Logger.Level.DEBUG);

	/** dialogue state */
	DialogueSystem system;

	/** HTTP client and URI for the speech recognition */
	HttpClient asrClient;
	URI asrURI;

	/** HTTP client and URI for the speech synthesis */
	HttpClient ttsClient;
	URI ttsURI;

	/** whether the system is paused or active */
	boolean paused = true;

	/** file used to save the speech input (leave empty to avoid recording) */
	public static String SAVE_SPEECH = "";


	/**
	 * Creates a new plugin, attached to the dialogue system
	 * 
	 * @param system the dialogue system to attach
	 * @throws DialException
	 */
	public NuanceSpeech(DialogueSystem system) throws DialException {
		this.system = system;
		List<String> missingParams = new LinkedList<String>(Arrays.asList("id", "key", "lang"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new DialException("Missing parameters: " + missingParams);
		}

		buildClients();

		if (system.getModule(GUIFrame.class) != null) {
			system.getModule(GUIFrame.class).enableSpeech(true);
		}
	} 

	/**
	 * Starts the Nuance speech plugin.
	 */
	@Override
	public void start() throws DialException {
		paused = false;
		GUIFrame gui = system.getModule(GUIFrame.class);
		if (gui == null) {
			throw new DialException("Nuance connection requires access to the GUI");
		}

		// quick hack to ensure that the audio capture works 
		try {
			SpeechStream firstStream = new SpeechStream(system.getSettings().inputMixer);
			Thread.sleep(100);
			firstStream.close(); 
		}
		catch (Exception e) { 
			e.printStackTrace();
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

		if (updatedVars.contains(speechVar) && state.hasChanceNode(speechVar) && !paused) {
			Value speechVal = system.getContent(speechVar).toDiscrete().getBest();
			if (speechVal instanceof SpeechStream) {
				new Thread(() -> {
					CategoricalTable table = recognise((SpeechStream)speechVal);
					if (!table.isEmpty()) {
						system.addContent(table);
					}
				}).start();
			}
		}
		else if (updatedVars.contains(outputVar) && state.hasChanceNode(outputVar) && !paused) {
			Value utteranceVal = system.getContent(outputVar).toDiscrete().getBest();
			if (utteranceVal instanceof StringVal) {
				synthesise(utteranceVal.toString());
			}
		}
	}



	/**
	 * Processes the audio data contained in tempFile (based on the recognition
	 * grammar whenever provided) and returns the corresponding N-Best list
	 * of results.
	 * 
	 * @param stream the speech stream containing the audio data
	 * @return the corresponding N-Best list of recognition hypotheses
	 */
	private CategoricalTable recognise(SpeechStream stream) {

		CategoricalTable table = new CategoricalTable(system.getSettings().userInput);

		int sampleRate =  (int)stream.getFormat().getSampleRate();
		log.info("calling Nuance server for recognition... "
				+ "(sample rate: " + sampleRate + " Hz.)" );       
		try {
			
			// wait until the stream is closed to save the audio data
			if (SAVE_SPEECH != null && SAVE_SPEECH.length() > 0) {
				while (!stream.isClosed()) {
					Thread.sleep(50);
				}
				stream.generateFile(new File(SAVE_SPEECH));
			}
			HttpPost httppost = new HttpPost(asrURI);
			String format = "audio/x-wav;codec=pcm;bit="+stream.getFormat().getFrameSize()*8 
					+ ";rate=" + sampleRate;
			String lang = system.getSettings().params.getProperty("lang");
			httppost.addHeader("Content-Type",  format);
			httppost.addHeader("Accept",  "application/xml");
			httppost.addHeader("Accept-Language", lang);
			InputStreamEntity reqEntity  = new InputStreamEntity(stream);
			reqEntity.setContentType(format);
			httppost.setEntity(reqEntity);			

			HttpResponse response = asrClient.execute(httppost);

			HttpEntity resEntity = response.getEntity();
			if (resEntity== null || response.getStatusLine().getStatusCode() != 200) {
				log.info("Response status: " + response.getStatusLine());
				return table;
			}

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(resEntity.getContent()));
			String sentence;
			Map<Value, Double> lines = new HashMap<Value, Double>();
			while((sentence = reader.readLine()) != null){
				lines.put(ValueFactory.create(sentence), 1.0/(lines.size()+1));
			}
			table.addRows(InferenceUtils.normalise(lines));
			reader.close();
		}
		catch (Exception e) {
			log.warning("could not extract ASR results: " + e);
		}
		return table;
	}




	/**
	 * Performs remote speech synthesis with the given utterance, and plays it on the 
	 * standard audio output.
	 * 
	 * @param utterance the utterance to synthesis
	 */
	public void synthesise(String utterance) {
		try {
			log.info("calling Nuance server for synthesis...\t"); 

			HttpPost httppost = new HttpPost(ttsURI);
			httppost.addHeader("Content-Type",  "text/plain");
			httppost.addHeader("Accept", "audio/x-wav;codec=pcm;bit=16;rate=16000");
			HttpEntity entity = new StringEntity(utterance, "utf-8");;
			httppost.setEntity(entity);

			HttpResponse response = ttsClient.execute(httppost);

			HttpEntity resEntity = response.getEntity();
			if (resEntity== null || response.getStatusLine().getStatusCode() != 200) {
				log.info("Response status: " + response.getStatusLine());
				return;
			}

			InputStream in = resEntity.getContent();

			ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[1024 * 16];
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				rawBuffer.write(data, 0, nRead);
			}
			rawBuffer.flush();

			AudioUtils.playAudio(rawBuffer.toByteArray(), system.getSettings().outputMixer);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Builds the REST clients for speech recognition and synthesis.
	 * 
	 * @throws DialException
	 */
	private void buildClients() throws DialException {

		// Initialize the HTTP clients
		asrClient = HttpClientBuilder.create().build();
		ttsClient = HttpClientBuilder.create().build();

		try {

			URIBuilder builder = new URIBuilder();
			builder.setScheme("https");
			builder.setHost("dictation.nuancemobility.net");
			builder.setPort(443);
			builder.setPath("/NMDPAsrCmdServlet/dictation");
			builder.setParameter("appId", system.getSettings().params.getProperty("id"));
			builder.setParameter("appKey", system.getSettings().params.getProperty("key"));
			builder.setParameter("id", system.getSettings().params.getProperty("0000"));
			asrURI = builder.build();
			builder.setHost("tts.nuancemobility.net");
			builder.setPath("/NMDPTTSCmdServlet/tts");
			builder.setParameter("ttsLang", system.getSettings().params.getProperty("lang"));
			ttsURI = builder.build();
		}
		catch (Exception e) {
			throw new DialException("cannot build client: " + e);
		}
	}



}

