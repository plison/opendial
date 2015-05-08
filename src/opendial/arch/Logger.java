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

package opendial.arch;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Utility for logging on the standard output (console).
 *
 * @author Pierre Lison plison@ifi.uio.no
 * 
 */
public class Logger {

	/** Logging levels */
	public static enum Level {
		NONE, /* no messages are shown */
		MIN, /* only severe errors are shown */
		NORMAL, /* severe errors, warning and infos are shown */
		DEBUG /* every message is shown, including debug */
	}

	// Label of the component to log
	String componentLabel;

	// logging level for this particular logger
	Level level;

	// print streams
	PrintStream out;
	PrintStream err;

	/**
	 * Create a new logger for the component, set at a given logging level
	 * 
	 * @param componentLabel the label for the component
	 * @param level the logging level
	 */
	public Logger(String componentLabel, Level level) {
		this.componentLabel = componentLabel;
		this.level = level;
		try {
			out = new PrintStream(System.out, true, "UTF-8");
			err = new PrintStream(System.err, true, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

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
			err.println("[" + componentLabel + "] SEVERE: " + s);
		}
	}

	public void severe(int nb) {
		severe("" + nb);
	}

	public void severe(float fl) {
		severe("" + fl);
	}

	/**
	 * Log a warning message
	 * 
	 * @param s the message
	 */
	public void warning(String s) {
		if (level == Level.NORMAL || level == Level.DEBUG) {
			err.println("[" + componentLabel + "] WARNING: " + s);
		}
	}

	public void warning(int nb) {
		warning("" + nb);
	}

	public void warning(float fl) {
		warning("" + fl);
	}

	/**
	 * Log an information message
	 * 
	 * @param s the message
	 */
	public void info(String s) {
		if (level == Level.NORMAL || level == Level.DEBUG) {
			out.println("[" + componentLabel + "] INFO: " + s);
		}
	}

	public void info(int nb) {
		info("" + nb);
	}

	public void info(float fl) {
		info("" + fl);
	}

	public void info(Object o) {
		info(o.toString());
	}

	/**
	 * Log a debugging message
	 * 
	 * @param s the message
	 */
	public void debug(String s) {
		if (level == Level.DEBUG) {
			out.println("[" + componentLabel + "] DEBUG: " + s);
		}
	}

	public void debug(int nb) {
		debug("" + nb);
	}

	public void debug(float fl) {
		debug("" + fl);
	}

	public void debug(Object o) {
		debug("" + o);
	}

}
