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

package opendial.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Utility for logging on the standard output (console).  
 *
 * @author  Pierre Lison plison@ifi.uio.no 
 * @version $Date:: 2011-09-27 17:54:53 #$
 *
 */
public class BasicConsoleLogger extends Logger {

	public static final boolean REPLACE_DEFAULT_CONSOLE_LOGGER = true;
	
	
	/**
	 * Inherited constructor for the console logger
	 * @param name
	 * @param resourceBundleName
	 */
	protected BasicConsoleLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}
 
	
	/**
	 * Create a new logger, with the name given as parameter 
	 * and a logging level
	 * 
	 * @param loggerName the logger
	 * @param level logging level
	 * @return the created logger
	 */
	public static Logger createLogger(String loggerName, Level level) {
		
		// create a new logger and add the simple log formatter to it
		Logger logger = getLogger(loggerName);
		ConsoleHandler errorHandler = new LogHandler();
		errorHandler.setFormatter(new LogFormatter());
		errorHandler.setLevel(Level.WARNING);
		logger.addHandler(errorHandler);

		
		if (REPLACE_DEFAULT_CONSOLE_LOGGER) {
			deleteDefaultLogger();
		}
		logger.setLevel(level);
		return logger;
	}
	
	
	
	/**
	 * Delete the default console logger
	 */
	private static void deleteDefaultLogger() {
		Logger rootLogger = getLogger("");

		for (Handler handler2 : rootLogger.getHandlers()) {
			rootLogger.removeHandler(handler2);
		}
	}

}


/**
 * Log console handler, using both system.err and system.out
 * (which means a easier-to-read output)
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
final class LogHandler extends ConsoleHandler {
	
	/**
	 * Publish a new log record, using using system.err for warning 
	 * and error messages, and system.out for all other messages
	 *
	 * @param rec the log record
	 */
	@Override
	public void publish(LogRecord rec) {

        try {
            String message = getFormatter().format(rec);
            if (rec.getLevel().intValue() >= Level.WARNING.intValue())
            {
                System.err.write(message.getBytes());                       
            }
            else
            {
                System.out.write(message.getBytes());
            }
        } catch (Exception exception) {
            reportError(null, exception, ErrorManager.FORMAT_FAILURE);
            return;
        }

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

	
	/**
	 * Format the log record according to the following convention:
	 *  [loggerName] recordLevel: message
	 *  
	 *  @param rec the log record
	 */
	@Override
	public String format(LogRecord rec) {
		return "["+rec.getLoggerName()+"] " + rec.getLevel() + ": " + rec.getMessage() + "\n";
	}
}

