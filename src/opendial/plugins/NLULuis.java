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

import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;

/**
 * Module for interfacing OpenDial with Microsoft's cloud-based natural 
 * language understanding service LUIS.
 * 
 * @author 	Svetlana Stoyanchev (svetana.stoyanchev@gmail.com), 
 * 			Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NLULuis implements Module {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;

	boolean paused = true;

	public static String userActVar = "a_u";
	
	public static String serverURL = "https://api.projectoxford.ai/luis/v1/application";		
	public static String charset = "UTF-8";

	// LUIS' subscription key
	String subscriptionkey;
	
	// LUIS' understanding model
	String model;

	/**
	 * Creates a new module to access LUIS' understanding service. Two parameters
	 * must be specified: the model (via the parameter model_luis) and the subscription
	 * key (via the parameter key_luis).
	 * 
	 * @param system the dialogue system to which to connect the service.
	 */
	public NLULuis(DialogueSystem system) {
		this.system = system;
		if (system.getSettings().params.containsKey("model_luis")) {
			this.model = system.getSettings().params.getProperty("model_luis") ;
		}
		else {
			throw new RuntimeException("parameter \"model_luis\" must be specified");
		}
		if (system.getSettings().params.containsKey("key_luis")) {
			this.subscriptionkey = system.getSettings().params.getProperty("key_luis") ;
		}
		else {
			throw new RuntimeException("parameter \"key_luis\" must be specified");
		}
	}
	
	/**
	 * Starts the module.
	 */
	@Override
	public void start() {
		paused = false;
	}

	/**
	 * If 
	 * @param state
	 * @param updatedVars
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (paused) {
			return;
		}
		String userVar = system.getSettings().userInput;
		String newVar = userActVar + "'";
		if (updatedVars.contains(userVar) && state.hasChanceNode(userVar)) {
			
			// if the user action is already specified with good confidence by another model/module,
			// we do not run the service.
			if (state.hasChanceNode(newVar) && state.queryProb(newVar).getProb("None") <0.3) {
				return;
			}
			
			// creating the conditional probability distribution
			ConditionalTable.Builder builder = new ConditionalTable.Builder(userActVar);

			// looping on the possible user utterances
			Set<Value> hypotheses = state.queryProb(userVar).getValues();
			for (Value hypothesis : hypotheses) {
				if (hypothesis instanceof StringVal) {
					String resp = makeRequest(hypothesis.toString().toLowerCase());
			//		log.info("Raw response="+resp.toString());
					LuisJson json = (new Gson()).fromJson(resp, LuisJson.class);
					
					// creating a new value as a list of (entity,type) pairs
					Set<Value> entities = new HashSet<Value>();
					for (Entity e : json.entities) {
						entities.add(ValueFactory.create("("+e.entity +"," + e.type + ")"));
					}
					
					Value outputVal = (entities.isEmpty())? 
							ValueFactory.none() : ValueFactory.create(entities);
					
					// adding the conditional probability in the table
					Assignment condition = new Assignment(userVar,hypothesis);
					builder.addRow(condition,outputVal,1.0);
				}
				else {
					Assignment condition = new Assignment(userVar,hypothesis);
					builder.addRow(condition,ValueFactory.none(),1.0);					
				}
			}
			log.info("adding " + builder.build() + " to the dialogue state");
			system.addContent(builder.build());
		}
	}


	/**
	 * Creates the request to the LUI service.
	 * 
	 * @param userUtt the utterance to analyse
	 * @return the resulting URL
	 */
	private String makeRequest(String userUtt) {
		try {
			OkHttpClient client = new OkHttpClient();

			String query = String.format("id=%s&subscription-key=%s&q=%s", 
					URLEncoder.encode(model, charset),
					URLEncoder.encode(subscriptionkey, charset),
					URLEncoder.encode(userUtt, charset));
			Request request = new Request.Builder()
					.url(serverURL+"?"+query)
					.build();
			Response response = client.newCall(request).execute();
			return response.body().string();
		}
		catch (Exception e) {
			e.printStackTrace();
			log.warning("Cannot make request " + userUtt);
			return "";
		}

	}

	/**
	 * Pauses or restarts the module
	 * 
	 * @param toPause true if the module should be paused, false otherwise
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true if the module is currently running, else false.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}
	
	
	/**
	 * Top-level data structure from the LUIS Json output
	 */
	final class LuisJson {
		String query;
		List<Intent> intents;
		List<Entity> entities;
	}
	
	/**
	 * Entity recognised in the LUIS Json output
	 */
	final class Entity {
		String entity;
		String type;
		Integer startIndex;
		Integer endIndex;
		Double score;
	}
	
	/**
	 * User intent in the LUIS Json output
	 */
	final class Intent {
		String intentTag;
		Double intentScore;
	}

}
