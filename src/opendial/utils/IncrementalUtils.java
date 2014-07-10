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
                                                                

package opendial.utils;


import java.util.HashMap;
import java.util.Map;

import opendial.arch.Logger;
import opendial.state.DialogueState;
import opendial.state.StatePruner;

/**
 * Utility method for incremental processing (creation of daemon threads that commit chance nodes
 * after a certain amount of time).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class IncrementalUtils {

	// logger
	public static Logger log = new Logger("IncrementalUtils", Logger.Level.DEBUG);
	
	
	// currently available daemons
	static Map<String,CommitDaemon> daemons = new HashMap<String,CommitDaemon>();
	
	
	/**
	 * Creates and starts a new commit daemon process
	 * 
	 * @param var the variable to monitor
	 * @param maxDurationGap the maximum duration to wait before commitment
	 * @param state the dialogue state
	 */
	public static void createDaemon (String var, long maxDurationGap, DialogueState state) {
		if (daemons.containsKey(var)) {
			daemons.get(var).maxDurationGap = Long.MAX_VALUE;
		}
		CommitDaemon daemon = new CommitDaemon(var, maxDurationGap, state);
		daemons.put(var, daemon);
		daemon.start();
	}
	
	
	
	/**
	 * Daemon process to monitor for committed nodes
	 */
	public static class CommitDaemon extends Thread {
		
		String var;
		long maxDurationGap;
		DialogueState state;
		
		private CommitDaemon(String var, long maxDurationGap, DialogueState state) {
			this.maxDurationGap = maxDurationGap;
			this.var = var;
			this.state = state;
		}
		
		public void run() {
			try {
				Thread.sleep(maxDurationGap);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (maxDurationGap == Long.MAX_VALUE) {
				return;
			}
			synchronized (state) {
			if (state.hasChanceNode(var) && !state.getChanceNode(var).isCommitted()) {
				state.getChanceNode(var).setAsCommitted(true);
				StatePruner.prune(state);
			}
			}
		}
	}

}

