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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
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
 * Module for interfacing OpenDial with Watson's cloud-based natural 
 * language understanding.
 * 
 * @author 	Svetlana Stoyanchev (svetana.stoyanchev@gmail.com), 
 * 			Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NLUWatson implements Module {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;

	boolean paused = true;

	public static String entitiesVar = "entities";

	public static String serverURL = "http://scorpion-5.2-3-5-7-11.com:8880";		
	public static String charset = "UTF-8";

	// Watson's understanding model
	String model;

	/**
	 * Creates a new module to access Watson's understanding service. One parameter
	 * must be specified: the model (via the parameter model_watson).
	 * 
	 * @param system the dialogue system to which to connect the service.
	 */
	public NLUWatson(DialogueSystem system) {
		this.system = system;
		if (system.getSettings().params.containsKey("model_watson")) {
			this.model = system.getSettings().params.getProperty("model_watson") ;
		}
		else {
			throw new RuntimeException("parameter \"model_watson\" must be specified");
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
		String newVar = entitiesVar + "'";
		if (updatedVars.contains(userVar) && state.hasChanceNode(userVar)) {

			// if the user action is already specified with good confidence by another model/module,
			// we do not run the service.
			if (state.hasChanceNode(newVar) && state.queryProb(newVar).getProb("None") <0.3) {
				return;
			}

			// creating the conditional probability distribution
			ConditionalTable.Builder builder = new ConditionalTable.Builder(entitiesVar);

			// looping on the possible user utterances
			Set<Value> hypotheses = state.queryProb(userVar).getValues();
			for (Value hypothesis : hypotheses) {
				if (hypothesis instanceof StringVal) {
					String resp = makeRequest(hypothesis.toString().toLowerCase());
					//		log.info("Raw response="+resp.toString());
					WatsonJson json = (new Gson()).fromJson(resp, WatsonJson.class);
					// creating a new value as a list of (entity,type) pairs
					Set<Value> entities = new HashSet<Value>();
					int segsize = 0;
					if (json!=null && json.segList!=null) {
						segsize = json.segList.size();
					}
					for (int segindex=0; segindex < segsize; segindex++) {
						Segment seg = json.segList.get(segindex);
						for (Entity e : seg.outAPI.entities) {
							String value = e.norm;
							if (value==null || value.equals("")) {
								value = e.entity;
							}
							entities.add(ValueFactory.create("("+value +"," + e.type + ")"));
						}
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
	 * Creates the request to the Watson service.
	 * 
	 * @param userUtt the utterance to analyse
	 * @return the resulting URL
	 */
	private String makeRequest(String userUtt) {
		try {

			OkHttpClient client = new OkHttpClient();

			RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), userUtt);

			Request request = new Request.Builder()
					.addHeader("Content-Type", "text/plain")
					.url(serverURL + "/" + model)
					.post(body)
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

	
	final class WatsonJson {
		@SerializedName("seg-list")  List<Segment> segList;
		@SerializedName("raw-text") String rawText;
		String hostname;
	}

	final class Segment{
		@SerializedName("NLUHotel") OutAPI outAPI;
	}

	 final class OutAPI{
			List<Entity> entities;
		  }

	final class Entity{

		Integer confidence;
		String entity;
		String type;
		String norm;

		List<Integer> wordindexes;
	}
	
	 

}
