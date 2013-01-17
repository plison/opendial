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

package opendial.common;


import opendial.arch.DialogueSystem;
import opendial.arch.Logger;

public class MiscUtils {

	// logger
	public static Logger log = new Logger("MiscUtils", Logger.Level.NORMAL);


	public static void waitUntilStable(DialogueSystem system) {
		try {
			Thread.sleep(30);
			int i = 0 ;
			while (!system.getState().isStable()) {
				Thread.sleep(30);
				i++;
				if (i > 30) {
					log.debug("dialogue state: " + system.getState().toString());
				}
			}
		}
		catch (InterruptedException e) { 
			log.debug ("interrupted exception: " + e.toString());
		}
	}
}

