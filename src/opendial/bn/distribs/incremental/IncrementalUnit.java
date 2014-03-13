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

package opendial.bn.distribs.incremental;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.*;

/**
 * Representation of an incremental unit, consisting of : <ul>
 * <li> a variable label (for instance, "u_u")
 * <li> an internal, automatically created identifier for the unit
 * <li> a "payload" (value)
 * <li> a timestamp for the creation of the unit
 * <li> a set of connections to previous incremental units (together with a probability value)
 * </ul>
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class IncrementalUnit {

	// logger
	public static Logger log = new Logger("IncrementalUnit", Logger.Level.DEBUG);

	// the unit identifier (created automatically)
	long id;
	
	// variable label
	String variable;
	
	// payload for the unit
	Value payload;
	
	// timestamp for the unit
	long timestamp;
	
	// set of connections to previous units (weighted by a probability)
	Map<Long,Double> previousUnits;

	
	/**
	 * Creates a new incremental unit.
	 * 
	 * @param variable variable label
	 * @param value payload (as a string)
	 */
	public IncrementalUnit(String variable, String value) {
		this(variable, ValueFactory.create(value));
	}
	
	/**
	 * Creates a new incremental unit.
	 * 
	 * @param variable variable label
	 * @param value payload
	 */
	public IncrementalUnit(String variable, Value value) {
		this.variable = variable;
		this.payload = value;
		previousUnits = new HashMap<Long, Double>();
		timestamp = System.currentTimeMillis();
		id = (new Random()).nextLong();
	}


	/**
	 * Returns the identifier for the unit
	 * 
	 * @return the identifier
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Returns the variable label
	 * 
	 * @return the label
	 */
	public String getVariable() {
		return variable;
	}

	/**
	 * Modifies the variable label
	 * 
	 * @param variable new label
	 */
	public void setVariable(String variable) {
		this.variable = variable;
	}

	/**
	 * Returns the payload for the unit
	 * 
	 * @return the payload
	 */
	public Value getPayload() {
		return payload;
	}

	/**
	 * Changes the payload for the unit
	 * 
	 * @param value the new payload
	 */
	public void setValue(Value value) {
		this.payload = value;
	}

	/**
	 * Returns the time stamp with the creation time of the unit
	 * (in milliseconds)
	 * 
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	
	/**
	 * Connects the unit to a previous incremental unit, with a specific
	 * probability for the connection
	 * @param previous the previous unit
	 * @param prob the probability for the connection
	 */
	public void connectTo(IncrementalUnit previous, double prob) {
		if (prob < 0.0 || prob > 1.0) {
			log.warning("probability " + prob  + " is ill-formed, cannot connect units");
			return;
		}
		previousUnits.put(previous.id, prob);
	}

	
	/**
	 * Returns the set of identifiers for the incremental units that 
	 * immediately precede the current unit
	 * 
	 * @return the set of identifiers for the previous units
	 */
	public Set<Long> getPreviousUnits() {
		return previousUnits.keySet();
	}
	
	
	/**
	 * Returns the connection probability between previousId and the current
	 * unit, if such a connection exists.  Else, return 0.
	 * 
	 * @param previousId the identifier for the previous incremental unit
	 * @return the corresponding probability
	 */
	public double getConnectionProbability(long previousId) {
		if (previousUnits.containsKey(previousId)) {
			return previousUnits.get(previousId);
		}
		else {
			log.warning(previousId + " is not connected to " + id);
			return 0.0;
		}
	}

	/**
	 * Returns a string representation of the unit
	 */
	@Override
	public String toString() {
		return variable + "+:" + payload.toString();
	}

	
	/**
	 * Returns the hashcode for the unit
	 */
	@Override
	public int hashCode() {
		return (new Long(id)).hashCode();
	}


	/**
	 * Returns true if the object is an incremental unit with the
	 * same identifier, and false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof IncrementalUnit) {
			return (((IncrementalUnit) o).id == id);
		}
		return false;
	}


	/**
	 * Copies the unit
	 * 
	 * @return the copy
	 */
	public IncrementalUnit copy() {
		IncrementalUnit unit = new IncrementalUnit(variable, payload.copy());
		unit.timestamp = timestamp;
		unit.id = id;
		unit.previousUnits = new HashMap<Long,Double>(previousUnits);
		return unit;
	}


}

