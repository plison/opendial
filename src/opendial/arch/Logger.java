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

package opendial.arch;

/**
 * Utility for logging on the standard output (console).  
 *
 * @author  Pierre Lison plison@ifi.uio.no 
 * @version $Date::                      $
 *  
 */
public class Logger {
	
	/** Logging levels */
	public static enum Level {
		NONE,  		/* no messages are shown */
		MIN,  		/* only severe errors are shown */
		NORMAL, 	/* severe errors, warning and infos are shown */
		DEBUG 		/* every message is shown, including debug */
	}  
	  
	// Label of the component to log
	String componentLabel;
	 
	// logging level for this particular logger
	Level level;

	
	/**
	 * Create a new logger for the component, set at a given
	 * logging level
	 * 
	 * @param componentLabel the label for the component
	 * @param level the logging level
	 */
	public Logger(String componentLabel, Level level) {
		this.componentLabel = componentLabel;
		this.level = level;
	}
	
	/**
	 * Modifies the logging level of the logger
	 * 
	 * @param level the new level
	 */
	public void setLevel(Level level) {
		this.level = level;
	}
	
	/**
	 * Log a severe error message
	 * 
	 * @param s the message
	 */
	public void severe(String s) {
		if (level != Level.NONE) {
		System.err.println("["+componentLabel+"] SEVERE: " + s);
		}
	}
	
	public void severe(int nb) { severe(""+nb); }
	public void severe(float fl) { severe(""+fl); }

	
	/**
	 * Log a warning message
	 * 
	 * @param s the message
	 */
	public void warning(String s) {
		if (level == Level.NORMAL || level == Level.DEBUG) {
		 System.err.println("["+componentLabel+"] WARNING: " + s);
		}
	}

	public void warning(int nb) { warning(""+nb); }
	public void warning(float fl) { warning(""+fl); }

	
	/**
	 * Log an information message
	 * 
	 * @param s the message
	 */
	public void info(String s) {
		if (level == Level.NORMAL || level == Level.DEBUG) {
		 System.out.println("["+componentLabel+"] INFO: " + s);
		}
	}

	public void info(int nb) { info(""+nb); }
	public void info(float fl) { info(""+fl); }
	public void info(Object o) { info(o.toString()); }
	
	/**
	 * Log a debugging message
	 * 
	 * @param s the message
	 */
	public void debug(String s) {
		if (level == Level.DEBUG) {
		 System.out.println("["+componentLabel+"] DEBUG: " + s);
		}
	}
	
	public void debug(int nb) { debug(""+nb); }
	public void debug(float fl) { debug(""+fl); }
	public void debug(Object o) { debug(""+o); }



}


