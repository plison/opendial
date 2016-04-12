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

import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALAutonomousMoves;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;

/**
 * 
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoASR implements Module {

	final static Logger log = Logger.getLogger("OpenDial");
 
	public static final float N_BEST = 5.0f;
 
	Session session;
	ALSpeechRecognition asr;
	ALMemory memory;
	Set<String> locks;

	DialogueSystem system;
	boolean paused = true;

	public NaoASR(DialogueSystem system) {
		this.system = system;

		List<String> params =
				new ArrayList<String>(Arrays.asList("grammar", "nao_ip"));
		params.removeAll(system.getSettings().params.keySet());
		if (!params.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + params);
		}
		try {
		session = NaoUtils.grabSession(system.getSettings());
		asr = new ALSpeechRecognition(session);
		memory = new ALMemory(session);
		}
		catch (Exception e) {
			log.warning("Cannot connect to Nao ASR: " + e.toString());
		}

		locks = new HashSet<String>();
	}

	@Override
	public void start() {
		log.info("starting up NAO ASR....");
		paused = false;

		try {
		asr.pause(false);
		asr.setAudioExpression(false);
		(new ALAutonomousMoves(session)).setExpressiveListeningEnabled(false);
		if (((ArrayList<?>)asr.getSubscribersInfo()).size() > 0) {
			asr.unsubscribe("naoASR");
		}
		String asrGrammar = system.getSettings().params.getProperty("grammar");
		String lcfFile =  asrGrammar.replace("bnf", "lcf");
		log.info("Using ASR grammar: " + asrGrammar);
		asr.compile(asrGrammar,lcfFile, "English");
		asr.removeAllContext();
		asr.addContext(lcfFile, "UserEval");
		asr.setParameter("Sensitivity", 0.8f);
		asr.setParameter("NbHypotheses", N_BEST);
		log.fine("ASR sensitivity: " + asr.getParameter("Sensitivity"));
		asr.subscribe("naoASR");
		memory.insertData("WordRecognized", "");
		asr.pause(false);
		log.fine("ASR successfully started!");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutting down Nao ASR");
			try {
				memory.unsubscribeToEvent("WordRecognized", "naoASR");
				asr.removeAllContext();
				asr.unsubscribe("naoASR");
				asr.pause(true);
			}
			catch (Exception e) {}
		}));
		memory.subscribeToEvent("WordRecognized", r -> processResult(r));
		}
		catch (Exception e) {
			log.warning("Cannot start Nao ASR: " + e.toString());
		}
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
	}

	@Override
	public void pause(boolean toPause) {
		try {
			if (toPause) {
				asr.pause(true);
			}
			else {
				asr.pause(false);
				memory.insertData("WordRecognized", "");
			}
		}
		catch (Exception e) {
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

	
	private void processResult(Object r) {

		if (!(r instanceof ArrayList)) {
			throw new RuntimeException("R is of class " + r.getClass().getSimpleName());
		}

		ArrayList<?> alist = (ArrayList<?>)r;
		final Map<String, Double> hypotheses = new HashMap<String, Double>();
		if (alist.size() > 0) {
			for (int i = 0; i < alist.size() / 2; i++) {
				String str = alist.get(i * 2).toString();
				double f = (Float) alist.get(i * 2 + 1);
				if (f > 0.1) {
					hypotheses.put(str, f);
				}
			}
		}
		if (!paused && !hypotheses.isEmpty()) {
			log.fine("initial hypotheses: " + hypotheses.toString());
			Map<String, Double> corrected = correctHypotheses(hypotheses);
			log.fine("corrected: " + corrected.toString().replace("\n", ", "));
			(new Thread(() -> system.addUserInput(corrected))).start();
		}
	}
	

	private Map<String, Double> correctHypotheses(
			Map<String, Double> initHypotheses) {

		float total = (float) Math.exp(3.0f);
		for (String initHyp : initHypotheses.keySet()) {
			total += (float) Math.exp(initHypotheses.get(initHyp) * 8);
			;
		}

		Map<String, Double> newHypotheses = new HashMap<String, Double>();
		for (String initHyp : initHypotheses.keySet()) {
			double newProb = (float) Math.exp(initHypotheses.get(initHyp) * 8);
			newHypotheses.put(initHyp, newProb / total);
		}
		return newHypotheses;
	}

}
