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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.modules.Module;
import opendial.state.DialogueState;


/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoASR implements Module, NaoEventListener {

	static Logger log = new Logger("NaoASR", Logger.Level.DEBUG);

	public static final int N_BEST = 3;

	NaoSession session;

	Set<String> locks;

	DialogueSystem system;
	boolean paused = true; 

	public NaoASR(DialogueSystem system) throws DialException {
		this.system = system;

		List<String> params = new ArrayList<String>(Arrays.asList("grammar", "nao_ip"));
		params.removeAll(system.getSettings().params.keySet());
		if (!params.isEmpty()) {
			throw new DialException("Missing parameters: " + params);
		}
		session = NaoSession.grabSession(system.getSettings());
		log.debug("connecting to Nao with address " + system.getSettings().params.get("nao_ip"));

		locks = new HashSet<String>();
	}

	/**
	 * @throws DialException 
	 *
	 */ 
	@Override
	public void start() throws DialException {
		log.info("starting up NAO ASR....");
		paused = false;

		session.call("ALSpeechRecognition", "pause", false);
		session.call("ALSpeechRecognition", "setAudioExpression", false);
		session.call("ALAutonomousMoves", "setExpressiveListeningEnabled", false);
		if (session.<ArrayList<?>>call("ALSpeechRecognition", "getSubscribersInfo").size() > 0) {
			session.call("ALSpeechRecognition", "unsubscribe", "naoASR");
		}
		String asrGrammar = system.getSettings().params.getProperty("grammar");
		log.info("Using ASR grammar: " + asrGrammar);
		session.call("ALSpeechRecognition", "compile", asrGrammar, 
				asrGrammar.replace("bnf", "lcf"), "English");
		session.call("ALSpeechRecognition", "removeAllContext");
		session.call("ALSpeechRecognition", "addContext", asrGrammar.replace("bnf", "lcf"), "UserEval");
		session.call("ALSpeechRecognition", "setParameter", "Sensitivity", 1);
		session.call("ALSpeechRecognition", "setParameter", "NbHypotheses", N_BEST);
		log.debug("ASR sensitivity: " + session.call("ALSpeechRecognition", "getParameter", "Sensitivity"));
		session.call("ALSpeechRecognition", "subscribe", "naoASR");
		session.call("ALMemory", "insertData", "WordRecognized", "");
		session.call("ALSpeechRecognition", "pause", false); 
		log.debug("ASR successfully started!");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {  
				System.out.println("Shutting down Nao ASR");
				try {  
					session.call("ALMemory", "unsubscribeToEvent", "WordRecognized", "naoASR");
					session.call("ALSpeechRecognition", "removeAllContext");
					session.call("ALSpeechRecognition", "unsubscribe", "naoASR");
					session.call("ALSpeechRecognition", "pause", true);
				}	catch (Exception e) {}
			}));
		session.listenToEvent("WordRecognized", this);
	}



	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {	}


	@Override
	public void pause(boolean toPause) {
		try {
			if (toPause) {
				session.call("ALSpeechRecognition", "pause", true);
			}
			else {
				session.call("ALSpeechRecognition", "pause", false);
				session.call("ALMemory", "insertData", "WordRecognized", "");	
			}
		} 
		catch (DialException e) {
			e.printStackTrace();
		}
		paused = toPause;
	}


	public void lockASR(String origin) {
		locks.add(origin);
		pause(true);
	}

	public void unlockASR(String origin) {
		locks.remove(origin);
		if (locks.isEmpty()) {
			pause(false);
		}
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void callback (Object event) {
		final Map<String,Float> hypotheses = new HashMap<String,Float>();

		ArrayList<?> val = new ArrayList();
		if (event instanceof ArrayList) {
			val.addAll(((List)event));
		}
		if (val.size() > 0) {
			for (int i = 0 ; i < val.size()/2 ; i++) {
				String str = val.get(i*2).toString();
				float f = (Float) val.get(i*2+1);
				if (f > 0.1) {
					hypotheses.put(str, f);
				}
			}
		} 

		if (!paused && !hypotheses.isEmpty()) {
			log.debug("initial hypotheses: " + hypotheses.toString());
			final CategoricalTable chypotheses = correctHypotheses(hypotheses);
			log.debug("corrected hypotheses: " + chypotheses.toString().replace("\n", ", "));
			Runnable runnable = () -> {
					try {
						system.addContent(chypotheses);
					}
					catch (DialException e) {
						log.warning("could not add content: " + e);
					}
			};
			(new Thread(runnable)).start();	
		}
	}
	

	

	private CategoricalTable correctHypotheses(Map<String,Float> initHypotheses) {

		float total = (float)Math.exp(3.0f);	
		for (String initHyp : initHypotheses.keySet()) {
			total += (float)Math.exp(initHypotheses.get(initHyp)*8);;
		}

		CategoricalTable newHypotheses = new CategoricalTable(system.getSettings().userInput);
		for (String initHyp : initHypotheses.keySet()) {
			float newProb = (float)Math.exp(initHypotheses.get(initHyp)*8);
			newHypotheses.addRow(initHyp, newProb/total);
		}
		return newHypotheses;
	}


}
