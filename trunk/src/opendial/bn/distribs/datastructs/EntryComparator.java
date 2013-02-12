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

package opendial.bn.distribs.datastructs;


import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import opendial.arch.Logger;
import opendial.bn.Assignment;

public class EntryComparator implements Comparator<Map.Entry<Assignment,Double>> {

	// logger
	public static Logger log = new Logger("ValueComparator",
			Logger.Level.NORMAL);

	@Override
	public int compare(Entry<Assignment, Double> arg0, Entry<Assignment, Double> arg1) {
		return (int)((arg0.getValue() - arg1.getValue())*1000);
	}

}

