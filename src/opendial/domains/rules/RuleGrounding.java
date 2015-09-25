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

package opendial.domains.rules;

import java.util.logging.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Representation of a set of possible groundings for a rule
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleGrounding {

	final static Logger log = Logger.getLogger("OpenDial");

	Set<Assignment> groundings;

	/**
	 * Constructs an empty set of groundings
	 */
	public RuleGrounding() {
		groundings = new HashSet<Assignment>();
		groundings.add(new Assignment());
	}

	/**
	 * Constructs a set of groundings with the given collection of alternative
	 * assignments
	 * 
	 * @param assigns the assignments
	 */
	public RuleGrounding(Collection<Assignment> assigns) {
		this();
		extend(assigns);
	}

	/**
	 * Adds a collection of alternative groundings. At least one must be non-failed.
	 * 
	 * @param alternatives the alternative groundings
	 */
	public void add(Collection<RuleGrounding> alternatives) {
		boolean foundSuccess = false;
		for (RuleGrounding g : alternatives) {
			if (!g.isFailed()) {
				add(g);
				foundSuccess = true;
			}
		}
		if (!foundSuccess) {
			groundings.clear();
		}
	}

	/**
	 * Adds a list of alternative groundings to the existing set
	 * 
	 * @param other the alternative groundings
	 */
	public void add(RuleGrounding other) {
		groundings.addAll(other.groundings);
		if (!isEmpty()) {
			groundings.remove(new Assignment());
		}
	}

	/**
	 * Adds a single assignment to the list of alternative groundings
	 * 
	 * @param singleAssign the assignment
	 */
	public void add(Assignment singleAssign) {
		if (singleAssign.isEmpty()) {
			return;
		}
		groundings.add(singleAssign);
		if (!isEmpty()) {
			groundings.remove(new Assignment());
		}
	}

	/**
	 * Extends the existing groundings with the provided assignment
	 * 
	 * @param assign the new assignment
	 */
	public void extend(Assignment assign) {
		if (assign.isEmpty()) {
			return;
		}
		groundings.stream().forEach(g -> g.addAssignment(assign));
	}

	/**
	 * Extends the existing groundings with the alternative groundings
	 * 
	 * @param other the next groundings to extend the current ones
	 */
	public void extend(RuleGrounding other) {
		if (other.isFailed()) {
			groundings.clear();
			return;
		}
		extend(other.getAlternatives());
	}

	/**
	 * Extends the existing groundings with the alternative groundings
	 * 
	 * @param alternatives the next groundings to extend the current ones
	 */
	public void extend(Collection<Assignment> alternatives) {
		if (alternatives.isEmpty()) {
			return;
		}
		Set<Assignment> newGroundings = new HashSet<Assignment>();
		for (Assignment o : alternatives) {
			for (Assignment g : groundings) {
				newGroundings.add(new Assignment(o, g));
				newGroundings.add(new Assignment(g, o));
			}
		}
		groundings = newGroundings;
	}

	/**
	 * Extend a set of groundings with the alternative values for the variable
	 * 
	 * @param variable the variable label
	 * @param vals the alternative values
	 */
	public void extend(String variable, Collection<Value> vals) {

		Set<Assignment> newGroundings = new HashSet<Assignment>();
		for (Assignment g : groundings) {
			for (Value v : vals) {
				newGroundings.add(new Assignment(g, variable, v));
			}
		}
		groundings = newGroundings;
	}

	/**
	 * Sets the groundings as failed (i.e. no possible groundings for the
	 * underspecified variables).
	 * 
	 */
	public void setAsFailed() {
		groundings.clear();
	}

	/**
	 * Returns the set of possible assignments
	 * 
	 * @return the possible assignments
	 */
	public Set<Assignment> getAlternatives() {
		return groundings;
	}

	public boolean isFailed() {
		return groundings.isEmpty();
	}

	/**
	 * Removes the given variables from the assignments
	 * 
	 * @param variables the variable labels
	 */
	public void removeVariables(Set<String> variables) {
		for (Assignment a : groundings) {
			a.removeAll(variables);
		}
	}

	/**
	 * Removes the given value from the assignments
	 * 
	 * @param value the variable value
	 */
	public void removeValue(Value value) {
		for (Assignment a : groundings) {
			a.removeValues(value);
		}
	}

	/**
	 * Copies the groundings
	 * 
	 * @return the copy
	 */
	public RuleGrounding copy() {
		return new RuleGrounding(
				groundings.stream().map(a -> a.copy()).collect(Collectors.toSet()));
	}

	/**
	 * Returns the hashcode of the set of alternative assignments
	 */
	@Override
	public int hashCode() {
		return groundings.hashCode();
	}

	/**
	 * Returns a string representation of the set of assignments
	 */
	@Override
	public String toString() {
		if (!isFailed()) {
			return groundings.toString();
		}
		else {
			return "failed";
		}
	}

	/**
	 * Returns true if o is a rule grounding with the same assignments, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof RuleGrounding) {
			return ((RuleGrounding) o).groundings.equals(groundings);
		}
		return false;
	}

	/**
	 * Returns true if the groundings are empty, false otherwise
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		return groundings.isEmpty() || (groundings.size() == 1
				&& groundings.iterator().next().isEmpty());
	}

}
