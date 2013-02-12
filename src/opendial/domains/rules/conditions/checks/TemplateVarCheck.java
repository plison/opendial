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

package opendial.domains.rules.conditions.checks;


import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.BasicCondition.Relation;

public class TemplateVarCheck extends AbstractCheck {

	// logger
	public static Logger log = new Logger("TemplateVarCheck",
			Logger.Level.DEBUG);

	Template variable;
	Template expectedVal;
	Relation rel;
		 
	public TemplateVarCheck(Template variable, Template expectedVal, Relation rel) {
		this.variable = variable;
		this.expectedVal = expectedVal;
		this.rel = rel;
	}
	
	@Override
	public boolean isSatisfied(Assignment input) {
		Template partialFill = variable.fillSlotsPartial(input);
		if (partialFill.getSlots().isEmpty()) {
			AbstractCheck instanceCheck = CheckFactory.createCheck(partialFill.getRawString(), expectedVal, rel);
			return instanceCheck.isSatisfied(input);
		}
		return false;
	}

	@Override
	public Assignment getLocalOutput(Assignment input) {
		Template partialFill = variable.fillSlotsPartial(input);
		if (partialFill.getSlots().isEmpty()) {
			AbstractCheck instanceCheck = CheckFactory.createCheck(partialFill.getRawString(), expectedVal, rel);
			return instanceCheck.getLocalOutput(input);
		}
		return new Assignment();
	}
}

