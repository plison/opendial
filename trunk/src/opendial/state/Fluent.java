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

package opendial.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.domains.types.GenericType;
import opendial.domains.types.FeatureType;
import opendial.utils.Logger;
import opendial.utils.StringUtils;

/**
 * Representation of a <i>fluent</i>, i.e. a probability distribution over a structured
 * state variable, which can change over time.
 * 
 * <p>A fluent is defined by a label, a type, a probability distribution over possible
 * values, and a (possibly empty) set of features attached to it.  
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Fluent {

	// logger
	static Logger log = new Logger("Fluent", Logger.Level.DEBUG);

	// type
	GenericType type;

	// label
	String label;

	// the distribution for the fluent
	SortedMap<String,Float> values;

	// the features attached to the fluent
	Map<String,ConditionalFluent> features;


	/**
	 * Creates a new empty fluent, given the declared type
	 * 
	 * @param type the declared type associated to the fluent
	 */
	public Fluent(GenericType type) {
		this.type = type;
		values = new TreeMap<String,Float>();
		features = new HashMap<String,ConditionalFluent>();
		label = type.getName();
		log.debug("new fluent created: " + label);
	}



	// ===================================
	//  SETTERS
	// ===================================


	/**
	 * Adds a value to the fluent
	 * 
	 * @param value the value to add
	 * @param prob its probability
	 * @throws DialException if value or probability is invalid
	 */
	public void addValue(String value, float prob) throws DialException {
		if (prob < 0.0 || prob > 1.0) {
			throw new DialException(prob + " is not a valid probability");
		}
		if (type.containsValue(value)) {
			values.put(value, prob);
		}
		else {
			throw new DialException("value " + value + 
					" not included in the type declaration for " + type.getName());
		}
	}


	/**
	 * Adds a collection of values to the fluent
	 * 
	 * @param values2 the values to add
	 * @throws DialException if one value or probability is invalid
	 */
	public void addValues(Map<String, Float> values) throws DialException {
		for (String val : values.keySet()) {
			addValue(val, values.get(val));
		}
	}


	/**
	 * Adds a feature to the fluent
	 * 
	 * @param feat the feature to add
	 */
	public void addFeature(ConditionalFluent feat) {
		features.put(feat.getLabel(), feat);
	}


	/**
	 * Adds a list of features to the fluent
	 * 
	 * @param features2 the features to add
	 */
	public void addFeatures(List<ConditionalFluent> features2) {
		for (ConditionalFluent feat : features2) {
			features.put(feat.getLabel(), feat);
		}
	}

	/**
	 * Sets the label of the fluent
	 * 
	 * @param label the label
	 */
	public void setLabel(String label) {
		this.label = label;
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the fluent type
	 * 
	 * @return the type
	 */
	public GenericType getType() {
		return type;
	}


	/**
	 * Returns the fluent label
	 * 
	 * @return
	 */
	public String getLabel() {
		return label;
	}


	/**
	 * Returns the fluent values
	 * 
	 * @return the values
	 */
	public SortedMap<String,Float> getValues() {
		return values;
	}

	/**
	 * Returns the fluent features
	 * 
	 * @return the features
	 */
	public List<ConditionalFluent> getFeatures() {
		return new ArrayList<ConditionalFluent>(features.values());
	}



	/**
	 * Returns the features defined for a particular base value
	 * 
	 * @param baseValue the base value
	 * @return all the features (full or partial) defined for the value
	 */
	public List<ConditionalFluent> getFeaturesForBaseValue(String baseValue) {

		List<ConditionalFluent> feats = new LinkedList<ConditionalFluent>();

		for (String featKey: features.keySet()) {
			ConditionalFluent fl = features.get(featKey);
			if (fl.getType() instanceof FeatureType && 
					((FeatureType)fl.getType()).isDefinedForBaseValue(baseValue)) {
				feats.add(fl);
			}
		}
		return feats;
	}



	/**
	 * Returns the full features (which are not partially defined)
	 * 
	 * @return the full features
	 */
	public List<ConditionalFluent> getFullFeatures() {

		List<ConditionalFluent> feats = new LinkedList<ConditionalFluent>();

		for (String featKey: features.keySet()) {
			ConditionalFluent fl = features.get(featKey);
			if (fl.getType() instanceof FeatureType && 
					!((FeatureType)fl.getType()).isPartial()) {
				feats.add(fl);
			}
		}
		return feats;
	}


	// ===================================
	//  PRINT OPERATIONS
	// ===================================


	
	/**
	 * Returns a string representation of the fluent
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return toString("");
	}


	/**
	 * Returns a string representation of the fluent,
	 * with an indent
	 * 
	 * @param indent the indent
	 * @return the string representation
	 */
	protected String toString(String indent) {
		
		// listing the label and type name
		String str = "";
		if (!type.isFixed() && !(type instanceof FeatureType)) {
			str += label + " (" + type.getName() + ")" ;
		}
		else {
			str += type.getName();
		}
		
		// looping on the values
		str += " = {";
		Iterator<String> valuesIt = values.keySet().iterator();
		while (valuesIt.hasNext()) {
			
			String v = valuesIt.next();
			str += valueToString(v, values.get(v));

			if (valuesIt.hasNext()) {
				str += ",\n" + indent + StringUtils.makeIndent(str.length());
			}
		}	
		if (values.keySet().isEmpty()) {	// if no values, add a dummy one
			str += valueToString("x", 1.0f);
		}
		str += "}";

		// listing the full features of the fluent
		str += fullFeatureToString();
		
		return str ;
	}


	/**
	 * Returns a string representation of the given fluent value + prob
	 * 
	 * @param value the value
	 * @param prob the probability
	 * @return
	 */
	public String valueToString(String value, float prob) {

		// show the value
		String str = value ;

		// add the arguments of the value (features)
		str += "(";
		for (ConditionalFluent fl : getFeaturesForBaseValue(value)) {
			str += fl.getType().getName();
			str += ",";	
		}
		str += ")";				

		// cleanup 
		if (str.endsWith("()")) { str = str.replace("()", ""); }
		else if (str.endsWith(",)")) { str = str.replace(",)", ")"); };

		// add the probability
		str += " [" + prob + "]";

		// add the definition of partial features
		str += " with ";
		for (ConditionalFluent fl : getFeaturesForBaseValue(value)) {
			if (((FeatureType)fl.getType()).isPartial()) {
				str += fl.toString(StringUtils.makeIndent(str.length())) + " and ";					
			}
		}

		// cleanup
		if (str.endsWith("with ")) { str = str.replace(" with ", ""); }
		else if (str.endsWith(" and ")) { str = str.substring(0, str.length() - 5); }

		return str;
	}


	/**
	 * Returns a string representation of the full features of the fluent
	 * 
	 * @return the string representation
	 */
	public String fullFeatureToString() {
		
		// listing the full features
		String str = " with ";
		for (ConditionalFluent fl: getFullFeatures()) {
			str += fl.toString(StringUtils.makeIndent(str.length()));
			str += " and ";
		}
		// cleanup
		if (str.endsWith("with ")) { str = str.replace(" with ", ""); }
		else if (str.endsWith(" and ")) { str = str.substring(0, str.length() - 5); }

		return str;
	}
	
	
	
	// ===================================
	//  OTHER UTILITY FUNCTIONS
	// ===================================

	

	/**
	 * Copy the fluent
	 * @return a copy of the fluent
	 */
	public Fluent copy() {

		Fluent copy = new Fluent(type);
		copy.setLabel(label);

		for (String v : values.keySet()) {
			try {
				copy.addValue(v, values.get(v));
			}
			catch (DialException e) {
				log.warning("Strange problem copying a fluent, aborting copy operation");
			}
		}
		for (ConditionalFluent f : features.values()) {
			copy.addFeature(f.copy());
		}
		return copy;
	}


	
}
