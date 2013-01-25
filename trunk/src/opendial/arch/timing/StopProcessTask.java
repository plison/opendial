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

package opendial.arch.timing;

import java.util.TimerTask;

import opendial.arch.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StopProcessTask extends TimerTask {

	// logger
	public static Logger log = new Logger("StopSamplingTask", Logger.Level.DEBUG);

	AnytimeProcess process;
	long timing;
	
	public StopProcessTask(AnytimeProcess query, long timing) {
		this.process = query;
		this.timing = timing;
	}
	
	/**
	 *
	 */
	@Override
	public void run() {
		if (!process.isTerminated()) {
			log.debug("time (" + timing + " ms.) has run out for process " + process.toString());
			process.terminate();
		}
	}
}
