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
import opendial.domains.types.EntityType;
import opendial.domains.types.AbstractType;
import opendial.domains.types.FeatureType;
import opendial.utils.Logger;

/**
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Fluent {

	static Logger log = new Logger("Fluent", Logger.Level.DEBUG);
	
	AbstractType type;
	
	String label;
	private static int entityCounter = 1;
	
	SortedMap<String,Float> values;
	
	Map<String,Fluent> features;
	
	
	public Fluent(AbstractType type) {
		this.type = type;
		values = new TreeMap<String,Float>();
		features = new HashMap<String,Fluent>();
		label = type.getName() + entityCounter;
		entityCounter++;
	}
	
	public void addValue(String value, float prob) throws DialException {
		if (prob < 0.0 || prob > 1.0) {
			throw new DialException(prob + " is not a valid probability");
		}
		if (type.acceptsValue(value)) {
			values.put(value, prob);
		}
		else {
			throw new DialException("value " + value + " not included in the type declaration for " + type.getName());
		}
	}

	/**
	 * 
	 * @param values2
	 * @throws DialException 
	 */
	public void addValues(Map<String, Float> values) throws DialException {
		for (String val : values.keySet()) {
			addValue(val, values.get(val));
		}
	}

	/**
	 * 
	 * @param features2
	 */
	public void addFeatures(List<Fluent> features2) {
		for (Fluent feat : features2) {
			features.put(feat.getLabel(), feat);
		}
	}
	
	
	public void addFeature(Fluent feat) {
		features.put(feat.getLabel(), feat);
	}

	/**
	 * 
	 * @return
	 */
	public AbstractType getType() {
		return type;
	}
	
	public String getLabel() {
		return label;
	}

	/**
	 * 
	 * @return
	 */
	public SortedMap<String,Float> getValues() {
		return values;
	}

	/**
	 * 
	 * @return
	 */
	public List<Fluent> getFeatures() {
		return new ArrayList<Fluent>(features.values());
	}

	
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * 
	 * @return
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
		for (Fluent f : features.values()) {
			copy.addFeature(f.copy());
		}
		return copy;
	}
	
	
	@Override
	public String toString() {
		return toString("");
	}
	
	
	private String toString(String indent) {
		String str = "";
		if (type instanceof EntityType) {
			str += label + " (" + type.getName() + ")" ;
		}
		else {
			str += type.getName();
		}
		str += " = {";
		
		indent += makeIndent(str.length());
				
		List<Fluent> fullFeats = getFullFeatures();

		Iterator<String> valuesIt = values.keySet().iterator();
		while (valuesIt.hasNext()) {
			String v = valuesIt.next();
			str += v ;
			
			List<Fluent> partialFeats = getPartialFeatures(v);
			List<Fluent> allFeats = mergeLists(fullFeats, partialFeats);
			
			if (!allFeats.isEmpty()) {
				Iterator<Fluent> it = allFeats.iterator();
				str += "(";
				while (it.hasNext()) {
					Fluent fl = it.next();
					str += fl.getType().getName();
					if (it.hasNext()) {
						str += ",";
					}
				}
				str += ")";				
			}
			str += " [" + values.get(v) + "]";
			
			if (!partialFeats.isEmpty()) {
				str += " with ";
				String addIndent = makeIndent(str.length());
				Iterator<Fluent> it = partialFeats.iterator();
				while (it.hasNext()) {
					Fluent fl = it.next();
					str += fl.toString(addIndent);
					if (it.hasNext()) {
						str += " and ";
					}
				}
			}
			if (valuesIt.hasNext()) {
				str += ",\n" + indent;
			}
		}
		
		str += "}";
		if (!fullFeats.isEmpty()) {
			str += " with ";
			String addIndent = makeIndent(str.length());
			Iterator<Fluent> it = fullFeats.iterator();
			while (it.hasNext()) {
				Fluent fl = it.next();
				str += fl.toString(addIndent);
				if (it.hasNext()) {
					str += " and ";
				}
			}
		}
		return str ;
	}

	/**
	 * 
	 * @param length
	 * @return
	 */
	private String makeIndent(int length) {
		String str = "";
		for (int i = 0 ; i < length ; i++) {
			str += " ";
		}
		return str;
	}

	/**
	 * 
	 * @param fullFeats
	 * @param partialFeats
	 * @return
	 */
	private List<Fluent> mergeLists(List<Fluent> list1, List<Fluent> list2) {
		List<Fluent> mergedList = new LinkedList<Fluent>();
		mergedList.addAll(list1);
		mergedList.addAll(list2);
		return mergedList;
	}

	/**
	 * 
	 * @return
	 */
	private List<Fluent> getFullFeatures() {
		List<Fluent> fullFeats = new LinkedList<Fluent>();
		
		for (Fluent f : features.values()) {
			if (!(f instanceof ConditionalFluent)) {
				fullFeats.add(f);
			}
		}
		return fullFeats;
	}

	/**
	 * 
	 * @param v
	 * @return
	 */
	public List<Fluent> getPartialFeatures(String baseValue) {
		
		List<Fluent> partialFeats = new LinkedList<Fluent>();
		
		for (Fluent f : features.values()) {
			if (f instanceof ConditionalFluent) {
				FeatureType type = (FeatureType) ((ConditionalFluent)f).getType();
				if (type.isDefinedForBaseValue(baseValue)) {
					partialFeats.add(f);
				}
			}
		}
		return partialFeats;
	}
}
