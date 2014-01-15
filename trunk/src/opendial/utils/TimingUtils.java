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

package opendial.utils;


import java.util.Timer;
import java.util.TimerTask;

import opendial.arch.AnytimeProcess;
import opendial.arch.Logger;

public class TimingUtils {

	// logger
	public static Logger log = new Logger("SystemUtils", Logger.Level.NORMAL);

	

	public static void setTimeout(final AnytimeProcess process, final long timeout) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				if (!process.isTerminated()) {
			log.debug("time (" + timeout + " ms.) has run out for sampling " + toString());
			process.terminate();
				}
			}
		}, timeout);
	}

}

