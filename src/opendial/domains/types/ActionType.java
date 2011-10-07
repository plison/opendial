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

package opendial.domains.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.domains.realisations.Realisation;
import opendial.domains.realisations.SurfaceRealisation;
import opendial.domains.types.values.BasicValue;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-10-07 16:21:33 #$
 *
 */
public class ActionType extends AbstractType {

	static Logger log = new Logger("ActionType", Logger.Level.NORMAL);

	
	// list of values for the entity
	Map<String,Realisation> actionValues;

		
	/**
	 * @param name
	 */
	public ActionType(String name) {
		super(name);
		actionValues = new HashMap<String,Realisation>();
	}
	
	public void addActionValue(Realisation action) {
		actionValues.put(action.getLabel(), action);
		internalAddValue(new BasicValue(action.getLabel()));
		if (action instanceof SurfaceRealisation && !((SurfaceRealisation)action).getSlots().isEmpty()) {
			for (String slot : ((SurfaceRealisation)action).getSlots()) {
				FeatureType feat = new FeatureType(slot);
				feat.addBaseValue(action.getLabel());
				addFeature(feat);
			}
		}
		
	}
	

	public Realisation getActionValue(String label) {
		return actionValues.get(label);
	}
	
	
	public List<Realisation> getActionValues() {
		return new ArrayList<Realisation>(actionValues.values());
	}

	/**
	 * 
	 * @param values
	 */
	public void addActionValues(List<Realisation> values) {
		for (Realisation value: values) {
			addActionValue(value);
		}
	}

}
