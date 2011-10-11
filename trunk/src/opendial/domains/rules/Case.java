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

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.utils.Logger;


/**
 * Representation of a rule case, containing a condition and
 * a list of alternative effects if the condition holds.  Each
 * alternative effect has a distinct probability.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Case {

	// logger
	static Logger log = new Logger("Case", Logger.Level.NORMAL);

	// the condition for the case
	Condition condition;
	
	// the list of alternative effects, together with their probability
	SortedMap<Effect,Float> effects;
	
	/**
	 * Creates a new case, with a void condition and an empty list of
	 * effects
	 */
	public Case() {
		condition = new VoidCondition();
		effects = new TreeMap<Effect,Float>();
	}
	
	/**
	 * Sets the condition for the case (if a condition is already specified,
	 * it is erased)
	 * 
	 * @param condition the condition
	 */
	public void setCondition(Condition condition) {
		this.condition = condition;
	}
	
	/**
	 * Adds an effects and its associated probability to the case
	 * 
	 * @param effect the effect
	 * @param prob the effect's probability
	 */
	public void addEffect(Effect effect, float prob) {
		effects.put(effect, prob);
	}

	
	/**
	 * Returns the condition for the case
	 * 
	 * @return the condition
	 */
	public Condition getCondition() {
		return condition;
	}
	
	
	/**
	 * Returns the list of alternative effects for the case
	 * 
	 * @return the list of alternative effects
	 */
	public List<Effect> getEffects() {
		return new ArrayList<Effect>(effects.keySet());
	}
	
	
	/**
	 * Returns the probability for a given effect in the case.  
	 * If the effect is not specified in the case, 0.0 is returned.
	 * 
	 * @param effect the effect
	 * @return the probability
	 */
	public float getProb(Effect effect) {
		if (effects.containsKey(effect)) {
			return effects.get(effect);
		}
		return 0.0f;
	}
	
}
