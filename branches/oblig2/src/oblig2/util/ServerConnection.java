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


package oblig2.util;


import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import oblig2.DialogueSystem;
import oblig2.NBest;
import oblig2.ConfigParameters;
import oblig2.actions.DialogueAction;
import oblig2.state.DialogueState;
import oblig2.state.DialogueStateListener;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Connection to the AT&T servers for speech recognition and synthesis.
 * The connection listens to the dialogue state, and reacts both to a 
 * newSpeechSignal event, and to a new system output.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ServerConnection implements DialogueStateListener {

	// logger
	public static Logger log = new Logger("ServerConnection", Logger.Level.NORMAL);

	// dialogue state
	DialogueState state;

	// parameters
	ConfigParameters parameters;

	/**
	 * Creates a new connection with the given dialogue state and parameters
	 * 
	 * @param state the state
	 * @param parameters the parameters
	 * @throws Exception if the connection could not be established
	 */
	public ServerConnection(DialogueSystem owner) throws Exception {
		this.parameters = owner.getParameters();
		this.state = owner.getDialogueState();
		state.addListener(this);

		if (parameters.doTesting) {
			testRecognition();
		}
	}

	
	private void testRecognition() throws Exception {
		log.info("testing connection to AT&T servers...");
		NBest nbest = recognise(new File(parameters.testASRFile));
		if (nbest.getHypotheses().isEmpty() || 
				!nbest.getHypotheses().get(0).getString().contains("one two three four five")) {
			throw new Exception ("Error, connection with AT&T servers could not be established");
		}
		else {
			log.debug("connection successfully established");
		}
	}
	/**
	 * Reacts to a new speech signal available for recognition by triggering
	 * the connection to the AT&T server
	 */
	@Override
	public void newSpeechSignal(File audioFile) {
		NBest nbest = recognise(audioFile); 
		log.debug("recognition complete, results: " + nbest);
		state.addUserUtterance(nbest);
	}


	@Override
	public void processUserInput(NBest u_u) {	}


	/**
	 * Reacts to a new system output to synthesise by triggering the connection
	 * to the AT&T server
	 *
	 * @param action the dialogue action containing the utterance
	 */
	@Override
	public void processSystemOutput(DialogueAction action) {
		synthesise (action.getUtterance());
	}
	
	
	/**
	 * Performs remote speech recognition on the AT&T server, by posting the audio
	 * file and waiting for the answer
	 * 
	 * @param filename filename for the audio file to send
	 * @return the N-Best list, if one could be received
	 */
	protected NBest recognise(File audioFile) {
		log.info("calling AT&T server...\t");       

		try {
			log.debug("open up connection");
			URL url = new URL("http://service.research.att.com/smm/watson" + 
					"?uuid="+parameters.uuid
					+"&cmd=rawoneshot" 
					+ "&appname="+parameters.appname
					+"&grammar="+parameters.grammar
					+ "&resultFormat=emma"
					+ "&control=set+config.nbest=" + parameters.nbest);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "audio/au");
			conn.setDoInput(true);
			conn.setDoOutput(true);

			OutputStream out = conn.getOutputStream();

			FileInputStream fin = new FileInputStream(audioFile);

			byte[] data = new byte[1024];
			int read = 0;
			while ((read=fin.read(data)) != -1) {
				out.write(data, 0, read);
			}

			fin.close();
			out.close();
			log.debug("finished sending audio");

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String fullResponse = "";
			String line;
			while ((line = rd.readLine()) != null) {
				fullResponse += line + "\n";
			}
			rd.close();

			if(conn != null) {
				conn.disconnect(); 
			}

			NBest nbest = extractNBest(fullResponse);
			log.info("recognition results successfully retrieved");
			log.debug("NBEST: " + nbest);
			return nbest;
		}
		catch (Exception e) {
			log.warning ("Error in server connection with AT&T servers " + e.toString() + " returning empty N-Best list");
			return new NBest();
		} 
	}


	/**
	 * Performs remote speech synthesis with the given utterance
	 * 
	 * @param utterance the utterance to synthesis
	 */
	protected void synthesise(String utterance) {

		try {
			URL url = new URL("http://service.research.att.com/smm/tts?uuid="+parameters.uuid
					+"&audioFormat=linear&appname="+ parameters.appname);

			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/plain");
			conn.setDoInput(true);
			conn.setDoOutput(true);

			OutputStream outText = conn.getOutputStream();
			outText.write(utterance.getBytes());
			outText.flush();

			// Get the response
			InputStream in = conn.getInputStream();

			OutputStream out = new FileOutputStream(parameters.tempTTSSoundFile);

			byte[] data = new byte[1024];
			int read = 0;
			while ((read=in.read(data)) != -1) {
				out.write(data,0,read);
			}
			out.close();
			in.close();

			if (parameters.activateSound) {
			(new AudioPlayer(parameters.tempTTSSoundFile)).start();
			}
		}
		catch (Exception e) {
			log.severe("Synthesis error"+ e.toString() + ", TTS operation is discarded");
		}
	}


	/**
	 * Extracts the N-Best list out of the XML-structured EMMA response
	 * returned by the AT&T server
	 * 
	 * @param plainResponse the response
	 * @return the extract N-Best, if any
	 */
	protected NBest extractNBest(String plainResponse) {

		NBest nbest = new NBest();

		try {
			// Create a factory
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Use document builder factory
			DocumentBuilder builder = factory.newDocumentBuilder();
			//Parse the document
			CharArrayReader reader=new CharArrayReader(plainResponse.toCharArray());
			Document doc = builder.parse(new org.xml.sax.InputSource(reader));


			NodeList nl = doc.getElementsByTagName("emma:interpretation");
			for (int i = 0 ; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				String tokens = n.getAttributes().getNamedItem("emma:tokens").getNodeValue();
				float conf = Float.parseFloat(n.getAttributes().getNamedItem("emma:confidence").getNodeValue());
				nbest.addHypothesis(tokens, conf);
			}
		}
		catch (Exception e) {
			log.warning ("Error in XML extraction: " + e.toString() + " returning empty nbest list");
		}
		return nbest;
	}


}
