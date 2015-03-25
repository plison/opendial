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

package opendial.domains.rules.effects;

import java.util.HashSet;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.TemplateCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;

/**
 * Representation of a basic effect of a rule.  A basic effect is formally
 * defined as a triple with: <ol>
 * <li> a (possibly underspecified) variable label;
 * <li> one of four basic operations on the variable SET, DISCARD, ADD;
 * <li> a (possibly underspecified) variable value;
 * </ol>
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public final class TemplateEffect extends BasicEffect {

	static Logger log = new Logger("TemplateEffect", Logger.Level.DEBUG);
	
	// variable label for the basic effect (as a template)
	final Template labelTemplate;
	
	// variable value for the basic effect  (as a template)
	final Template valueTemplate;

	// ===================================
	//  EFFECT CONSTRUCTION
	// ===================================
	
	
	
	/**
	 * Constructs a new basic effect, with a variable label, value, and type
	 * 
	 * @param variable variable label (raw string, possibly with slots)
	 * @param value variable value (raw string, possibly with slots)
	 * @param type type of effect
	 */
	public TemplateEffect(Template variable, Template value, EffectType type){
		this(variable,value,type, 1);
	}
	
	
	/**
	 * Constructs a new basic effect, with a variable label, value, and type
	 * 
	 * @param variable variable label (raw string, possibly with slots)
	 * @param value variable value (raw string, possibly with slots)
	 * @param type type of effect
	 */
	public TemplateEffect(Template variable, Template value, EffectType type, int priority){
		super(variable.toString(), (value.getSlots().isEmpty())? ValueFactory.none() : 
			ValueFactory.create(value.getRawString()), type, priority);
		this.labelTemplate = variable;
		this.valueTemplate = value;
	}


	/**
	 * Ground the slots in the effect variable and value (given the assignment)
	 * and returns the resulting effect.
	 * 
	 * @param grounding the grounding
	 * @return the grounded effect
	 */
	@Override
	public BasicEffect ground(Assignment grounding) {
		Template newT = labelTemplate.fillSlots(grounding);
		Template newV = valueTemplate.fillSlots(grounding);
		if (newT.isUnderspecified() || (!newV.getSlots().isEmpty())) {
			return new TemplateEffect(newT, newV, type, priority);
		}
		else {
			return new BasicEffect(newT.getRawString(), newV.getRawString(), type, priority);
		}
		
	}
	
	
	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns the set of additional input variables for the effect (from slots 
	 * in the variable label and value).
	 *
	 * @return sets of input variables
	 */
	@Override
	public Set<String> getSlots() {
		Set<String> additionalInputVariables = new HashSet<String>();
		additionalInputVariables.addAll(this.labelTemplate.getSlots());
		additionalInputVariables.addAll(this.valueTemplate.getSlots());
		return additionalInputVariables;
		}

	

	/**
	 * Returns true if the effect contains slots to fill, and false otherwise
	 */
	@Override
	public boolean containsSlots() {
		return !labelTemplate.getSlots().isEmpty() || !valueTemplate.getSlots().isEmpty();
	}
	
	/**
	 * Converts the basic effect into an equivalent condition.
	 * 
	 * @return the equivalent (basic or template-based) condition
	 */
	@Override
	public Condition convertToCondition() {
		Relation r = (type == EffectType.DISCARD)? Relation.UNEQUAL : Relation.EQUAL;
		return new TemplateCondition(labelTemplate, valueTemplate, r);
	}


	/**
	 * Returns the template representation of the variable label
	 * 
	 * @return the variable template
	 */
	public Template getVariableTemplate() {
		return labelTemplate;
	}
	

	/**
	 * Returns the template representation of the variable value
	 * 
	 * @return the value template
	 */
	public Template getValueTemplate() {
		return valueTemplate;
	}

	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns the string representation of the basic effect
	 */
	@Override
	public String toString() {
		String str = labelTemplate.toString();
		switch (type) {
		case SET: str += ":="; break;
		case DISCARD: str += "!="; break;
		case ADD: str += "+="; break;
		}
		str += valueTemplate.toString();
		return str;
	}
	
	
	/**
	 * Returns the hashcode for the basic effect
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return labelTemplate.hashCode() ^ type.hashCode() ^ priority ^ valueTemplate.hashCode();
	}

	
	/**
	 * Returns true if the object o is a basic effect that is identical to the
	 * current instance, and false otherwise.
	 *
	 * @param o the object to compare
	 * @return true if the objects are identical, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof TemplateEffect) {
			if (!((TemplateEffect)o).labelTemplate.equals(labelTemplate)) {
				return false;
			}
			else if (!((TemplateEffect)o).valueTemplate.equals(valueTemplate)) {
				return false;
			}
			else if (!((TemplateEffect)o).getType().equals(type)) {
				return false;
			}
			else if (((TemplateEffect)o).priority != priority) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns a copy of the effect.
	 * 
	 * @return the copy.
	 */
	@Override
	public TemplateEffect copy() {
		return new TemplateEffect(labelTemplate, valueTemplate, type, priority);
	}


	/**
	 * Returns a copy of the effect with a new priority
	 * @param priority the new priority
	 * @return a new basic effect with the changed priority
	 */
	@Override
	public TemplateEffect changePriority(int priority) {
		return new TemplateEffect(labelTemplate, valueTemplate, type, priority);	
	}

	
}
