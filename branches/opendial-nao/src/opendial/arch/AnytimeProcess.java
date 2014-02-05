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


	public Logger log = new Logger("AnytimeProcess", Logger.Level.NORMAL);

	/**
	 * Creates a new anytime process with the given timeout (in milliseconds)
	 * 
	 * @param timeout the maximum duration of the process
	 */
	public AnytimeProcess(final long timeout) {
		final Class<? extends AnytimeProcess> cls = this.getClass();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (!isTerminated()) {
					log.debug("Time (" + timeout + " ms.) has run out for " + cls.getSimpleName());
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

