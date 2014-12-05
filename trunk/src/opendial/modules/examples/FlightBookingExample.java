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

package opendial.modules.examples;

import java.util.Collection;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * Example of simple external module used for the flight-booking dialogue domain.
 * The module monitors for two particular values for the system action:<ol>
 * <li>"FindOffer" checks the (faked) price of the user order and returns MakeOffer(price)
 * <li>"Book" simulates the booking of the user order.
 * </ol>
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $ *
 */
public class FlightBookingExample implements Module {

	// logger
	public static Logger log = new Logger("FlightBookingExample", Logger.Level.DEBUG);

	// the dialogue system
	DialogueSystem system;
	
	// whether the module is paused or active
	boolean paused = true;

	/**
	 * Creates a new instance of the flight-booking module
	 * 
	 * @param system the dialogue system to which the module should be attached
	 */
	public FlightBookingExample(DialogueSystem system) {
		this.system = system;
	}

	/**
	 * Starts the module.
	 */
	@Override
	public void start() throws DialException {
		paused = false;
	}

	/**
	 * Checks whether the updated variables contains the system action and (if yes)
	 * whether the system action value is "FindOffer" or "Book".  If the value is 
	 * "FindOffer", checks the price of the order (faked here to 179 or 299 EUR) 
	 * and adds the new action "MakeOffer(price)" to the dialogue state.  If the
	 * value is "Book", simply write down the order on the system output.
	 * 
	 * @param state the current dialogue state
	 * @param updatedVars the updated variables in the state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (updatedVars.contains("a_m") && state.hasChanceNode("a_m")) {
			String action = state.queryProb("a_m").toDiscrete().getBest().toString();
			
			if (action.equals("FindOffer")) {
				String returndate = state.queryProb("ReturnDate").toDiscrete().getBest().toString();
				
				// here, we fake the price estimation by making up numbers.  Obviously,
				// the prices should be derived from a database in a real system.
				int price = (returndate.equals("NoReturn"))? 179 : 299;
				String newAction="MakeOffer(" + price + ")";
				system.addContent(new Assignment("a_m", newAction));
			}
			else if (action.equals("Book")) {
					
				String departure = state.queryProb("Departure").toDiscrete().getBest().toString();
				String destination = state.queryProb("Destination").toDiscrete().getBest().toString();
				String date = state.queryProb("Date").toDiscrete().getBest().toString();
				String returndate = state.queryProb("ReturnDate").toDiscrete().getBest().toString();
				String nbtickets = state.queryProb("NbTickets").toDiscrete().getBest().toString();
				
				// In a real system, the system database should be modified here to 
				// actually perform the booking.  Here, we just print a small message.
				String info = "Booked " + nbtickets + " tickets from " + departure + " to " 
						+ destination + " on " + date 
						+ ((returndate.equals("NoReturn"))? " and return on " + returndate : "");
				log.info(info);
			}
		}

	}

	/**
	 * Pauses the module.
	 * 
	 * @param toPause whether to pause the module or not
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}
	
	/**
	 * Returns whether the module is currently running or not.
	 * 
	 * @return whether the module is running or not.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

}
