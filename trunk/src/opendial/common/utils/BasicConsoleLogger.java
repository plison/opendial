// =================================================================                                                                   
// Copyright (C) 2009-2011 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.common.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Utility for logging on the standard output (console).  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicConsoleLogger extends Logger {

	protected BasicConsoleLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}
 
	public static Logger getDefaultLogger() {

		Logger l = getLogger("");

		for (Handler handler2 : l.getHandlers()) {
			l.removeHandler(handler2);
		}

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new LogFormatter());
		l.addHandler(handler);

		return l;
	}

}


/**
 * String formatter associated with the console logger
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $date::                      $
 *
 */
final class LogFormatter extends Formatter {

	@Override
	public String format(LogRecord arg0) {
		return "["+arg0.getClass().getSimpleName()+"] " + arg0.getLevel() + ": " + arg0.getMessage();
	}
}

