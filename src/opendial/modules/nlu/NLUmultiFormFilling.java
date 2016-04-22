// =================================================================                                                                   
// Copyright (C) 2016 Svetlana Stoyanchev

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

package opendial.modules.nlu;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.nlu.NLUAbstractJson;
import opendial.nlu.NLUAbstractRequester;
import opendial.nlu.luis.NLULuisJson;
import opendial.nlu.luis.NLULuisRequester;
import opendial.nlu.watson.NLUWatsonJson;
import opendial.nlu.watson.NLUWatsonRequester;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;


/**
 * Performs NLU on a user utterance
 * handles single or multiple forms
 * 
 * i
 * requires settings: nlu, fieldnames, fieldtypes
 * 
 * @author Svetlana Stoyanchev (svetana.stoyanchev@gmail.coom)
 */

public class NLUmultiFormFilling implements Module {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	// the dialogue system
	DialogueSystem system;
	


	// whether the module is paused or active
	boolean paused = true;
	
	
	ArrayList<NLUForm> nluFormList = new ArrayList();
	int numforms = 0;
	
	
	/**
	 * Creates a new instance of a form handling module 
	 * read the numforms setting
	 * create form objects for handling each form
	 * 
	 * @param system the dialogue system to which the module should be attached
	 */
	public NLUmultiFormFilling(DialogueSystem system) throws Exception{
		this.system = system;
		
		Settings settings = system.getSettings();
		

		if (settings.params.containsKey("numforms")) {
			numforms = new Integer(settings.params.getProperty("numforms"));
		}

		for (int index = 0; index < numforms; index++)
		{
        	NLUForm nluForm = new NLUForm(settings, index );
        	nluFormList.add(nluForm);        	
        }

		log.info("NLUmultiFormFilling: Initialized System with " + String.valueOf(numforms) + " forms." );
	}
	


	/**
	 * Starts the module.
	 */
	@Override
	public void start() {
		paused = false;
	}

	/**
	 * NLU end-point to retrieve intents the form that was set up
	 * 
	 * @param state the current dialogue state
	 * @param updatedVars the updated variables in the state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		
		log.info("NLUmultiFormFilling.Trigger");
		String contentStr = ""; 
		if(updatedVars.contains("statNLU")) {
			
			String indexStr = state.queryProb("statNLU").getBest().toString();
			log.info("NLUmultiFormFilling.Trigger, statNLU=" +indexStr);
			int index = new Integer(indexStr).intValue();
			if (index <=  nluFormList.size()){
				log.info("num forms = " + nluFormList.size() + " index=" + indexStr);
				contentStr = nluFormList.get(new Integer(index-1)).process(state,updatedVars);
			}
			else{
				log.log(Level.SEVERE, "NLU set for form out of bounds " + indexStr);
				
			}
			
			log.info("Adding content "+ contentStr);
			system.addContent(Assignment.createFromString(contentStr));

			log.info("NLUmodule.Trigger, u_u; state=" + state.toString());
				
		}			
	}

	/**
	 * Pauses the module.
	 * 
	 * @param toPause whether to pause the module or not
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns whether the module is currently running or not.
	 * 
	 * @return whether the module is running or not.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

}
