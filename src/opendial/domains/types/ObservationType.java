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

import opendial.domains.triggers.SurfaceTrigger;
import opendial.domains.triggers.Trigger;
import opendial.domains.types.values.BasicValue;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2011-10-07 16:21:33 #$
 *
 */
public class ObservationType extends AbstractType {

	static Logger log = new Logger("ObservationType", Logger.Level.NORMAL);

	Trigger trigger;
	
	/**
	 * @param name
	 */
	public ObservationType(String name, Trigger trigger) {
		super(name);
		this.trigger = trigger;
		internalAddValue(new BasicValue("true"));
		internalAddValue(new BasicValue("false"));		
		
		if (trigger instanceof SurfaceTrigger && !((SurfaceTrigger)trigger).getSlots().isEmpty()) {
			for (String slot : ((SurfaceTrigger)trigger).getSlots()) {
				FeatureType feat = new FeatureType(slot);
				addFeature(feat);
			}
		}
	}
	
	
	public Trigger getTrigger() {
		return trigger;
	}

}
