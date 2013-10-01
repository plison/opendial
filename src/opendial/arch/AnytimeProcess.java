// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.arch;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Interface for an "anytime" process -- that is, a process that can be interrupted
 * at any time and yield a result.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public abstract class AnytimeProcess extends Thread {
   
	
	public Logger log = new Logger("AnytimeProcess", Logger.Level.DEBUG);

	/**
	 * Creates a new anytime process with the given timeout (in milliseconds)
	 * 
	 * @param timeout the maximum duration of the process
	 */
	public AnytimeProcess(final long timeout) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				if (!isTerminated()) {
			log.debug("time (" + timeout + " ms.) has run out for sampling " + toString());
			terminate();
				}
			}
		}, timeout);
	}
	
	/**
	 * Terminates the process
	 */
	public abstract void terminate() ;
	
	/**
	 * Returns true if the process is terminated, and false otherwise
	 * 
	 * @return true if terminated, false otherwise
	 */
	public abstract boolean isTerminated();
		
}

