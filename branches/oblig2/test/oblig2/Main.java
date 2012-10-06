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

package oblig2;

import oblig2.util.Logger;

/**
 * Main class starting the dialogue system
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Main {

	// logger
	public static Logger log = new Logger("Main", Logger.Level.NORMAL);
	
	// AT&T parameters
	static String uuid = "F9A9D13BC9A811E1939C95CDF95052CC";
	static String	appname = "def001";
	static String	grammar = "numbers";

	/**
	 * Runs the dialogue system.
	 * 
	 * @param args no arguments
	 */
	public static void main(String[] args) {
		
		// basic parameters
		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
		
		// should be changed to your own policy!
		DialoguePolicy policy = new BasicPolicy();
		
		// starts up the system
		DialogueSystem system = new DialogueSystem(policy, parameters);
		system.start();
	}
}
