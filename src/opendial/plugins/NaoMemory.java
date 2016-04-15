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
import java.util.List;
import java.util.logging.Logger;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALMemory;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.BooleanVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.Module;

public class NaoMemory implements Module {

	final static Logger log = Logger.getLogger("OpenDial");

	Session session;
	ALMemory memory;

	DialogueSystem system;
	boolean paused = true;

	List<String> varsToMonitor = Arrays.asList("carried");

	public NaoMemory(DialogueSystem system) {
		this.system = system;
		try {
			session = NaoUtils.grabSession(system.getSettings());
			memory = new ALMemory(session);
		}
		catch (Exception e) {
			log.warning("Could not initialise NaoMemory: " + e.toString());
		}
	}

	@Override
	public void start() {
		paused = false;
		trigger(system.getState(), system.getState().getChanceNodeIds());
	}


	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (paused) {
			return;
		}
		for (String var : updatedVars) {
			if (varsToMonitor.contains(var)) {
				continue;
			}
			try {
				if (!state.hasChanceNode(var) && memory.getDataListName().contains(var)) {
					memory.removeData(var);
					continue;
				}
				Value v = system.getContent(var).getBest();
				if (v instanceof BooleanVal) {
					memory.insertData(var, ((BooleanVal)v).getBoolean()? 1 : 0);
				}
				else if (v instanceof DoubleVal) {
					memory.insertData(var, ((DoubleVal)v).getDouble());		
				}
				else {
					memory.insertData(var, v.toString());	
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				log.warning("Could not update memory for variable " + var + ": " + e);
			}
		}
		for (String var : varsToMonitor) {		
			try {
				if (memory.getDataListName().contains(var)) {
					Value v = ValueFactory.create(memory.getData(var).toString());
					if (!state.hasChanceNode(var) || !v.equals(system.getContent(var).getBest())) {
						system.addContent(var, v);
					}
				}
				else if (state.hasChanceNode(var) && !memory.getDataListName().contains(var)) {
					system.removeContent(var);
				}
			}
			catch (CallError | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}


}
