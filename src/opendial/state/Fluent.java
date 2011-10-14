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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import opendial.arch.DialException;
import opendial.domains.Type;
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
	Type type;

	// label
	String label;

	// the distribution for the fluent
	SortedMap<Object,Float> values;

	// the features attached to the fluent
	Map<String,Fluent> features;


	// entity counter for each entity                                                  
    private static Map<String,Integer> entityCounter = new HashMap<String,Integer>();


    
	/**
	 * Creates a new empty fluent, given the declared type
	 * 
	 * @param type the declared type associated to the fluent
	 */
	public Fluent(Type type) {
		this.type = type;
		values = new TreeMap<Object,Float>();
		features = new HashMap<String,Fluent>();
		
		if (type.isFixed()) {
		label = type.getName();
		}
		else {
			label = forgeNewLabel();
		}
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
	public void addValue(Object value, float prob) throws DialException {
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
	 * @param values the values to add
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
	public void addFeature(Fluent feat) {
		features.put(feat.getLabel(), feat);
		feat.setLabel(feat.getLabel() + "(" + label  + ")");
	}


	/**
	 * Adds a list of features to the fluent
	 * 
	 * @param features the features to add
	 */
	public void addFeatures(List<Fluent> features) {
		for (Fluent feat : features) {
			addFeature(feat);
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




	/**
	 * Sets the existence probability of the entity
	 * 
	 * @param existenceProb existence probability
	 * @throws DialException if problem occurs with the feature insertion
	 */
	public void setExistenceProb(float existenceProb) throws DialException {
		Type type = new Type("Exists");
		type.addValues(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
		type.setAsFixed(true);
		Fluent existenceFluent = new Fluent(type);
		existenceFluent.addValue(Boolean.TRUE, existenceProb);
		existenceFluent.addValue(Boolean.FALSE, 1.0f - existenceProb);		
		features.put(existenceFluent.getLabel(), existenceFluent);
	}
	
	
	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the fluent type
	 * 
	 * @return the type
	 */
	public Type getType() {
		return type;
	}


	/**
	 * Returns the fluent label
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}


	/**
	 * Returns the fluent values
	 * 
	 * @return the values
	 */
	public SortedMap<Object,Float> getValues() {
		return values;
	}
	
	/**
	 * Returns the probability associated with the given value,
	 * if one exists.  Else, return 0.0f;
	 * 
	 * @param value
	 * @return
	 */
	public float getProb(Object value) {
		if (values.containsKey(value)) {
			return values.get(value);
		}
		else {
			return 0.0f;
		}
	}

	/**
	 * Returns the fluent features
	 * 
	 * @return the features
	 */
	public List<Fluent> getFeatures() {
		return new ArrayList<Fluent>(features.values());
	}

	
	/**
	 * Returns the fluent features compatible with the value
	 * 
	 * @return all the features (full or partial) defined for the value
	 */
	public List<Fluent> getFeatures(Object baseValue) {
		List<Fluent> feats = new LinkedList<Fluent>();
		feats.addAll(getPartialFeatures(baseValue));
		feats.addAll(getFullFeatures());
		return feats;
	}
	

	/**
	 * Returns the features defined for a particular base value
	 * 
	 * @param baseValue the base value
	 * @return all the partial features defined for the value
	 */
	public List<Fluent> getPartialFeatures(Object baseValue) {

		List<Fluent> feats = new LinkedList<Fluent>();

		for (String feat : features.keySet()) {
			if (type.hasPartialFeature(feat, baseValue)) {
				feats.add(features.get(feat));
			}
		}
		return feats;
	}



	/**
	 * Returns the full features (which are not partially defined)
	 * 
	 * @return the full features
	 */
	public List<Fluent> getFullFeatures() {

		List<Fluent> feats = new LinkedList<Fluent>();

		for (String featKey: features.keySet()) {
			if (type.hasFullFeature(featKey)) {
				feats.add(features.get(featKey));
			}
		}
		return feats;
	}

	 
	/**
	 * Returns the existence probability of the entity, if one is
	 * defined.  Otherwise, returns 1.0f.
	 * 
	 * @return the existence probability
	 */
	public float getExistenceProb() {
		if (features.containsKey("Exists")) {
			return features.get("Exists").getProb(Boolean.TRUE);
		}
		else {
			return 1.0f;
		}
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
		if (!type.isFixed()) {
			str += label + " (" + type.getName() + ")" ;
		}
		else {
			str += type.getName();
		}
		
		// looping on the values
		str += " = {";
		Iterator<Object> valuesIt = values.keySet().iterator();
		while (valuesIt.hasNext()) {
			
			Object v = valuesIt.next();
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
	 * @return the string representation of the value
	 */
	public String valueToString(Object value, float prob) {

		// show the value
		String str = value.toString() ;

		// add the arguments of the value (features)
		str += "(";
		for (Fluent fl : getFeatures(value)) {
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
		for (Fluent fl : getPartialFeatures(value)) {
				str += fl.toString(StringUtils.makeIndent(str.length())) + " and ";					
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
		for (Fluent fl: getFullFeatures()) {
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

		for (Object v : values.keySet()) {
			try {
				copy.addValue(v, values.get(v));
			}
			catch (DialException e) {
				log.warning("Strange problem copying a fluent, aborting copy operation");
			}
		}
		for (Fluent f : features.values()) {
			Fluent f2 = new Fluent(f.getType());
			copy.addFeature(f2);
		}
		return copy;
	}

	
    /**                                                                                
     * Forge a new label, by concatenating the type label + a counter                  
     * on the number of entities                                                       
     *                                                                                 
     * @return                                                                         
     */
    private String forgeNewLabel() {
            String typeName = type.getName();
            if (!entityCounter.containsKey(typeName)) {
                    entityCounter.put(typeName, 1);
            }
            else {
                    entityCounter.put(typeName, entityCounter.get(typeName) + 1);
            }
            return typeName + entityCounter.get(typeName);
    }


	
}
