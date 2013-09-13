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

package opendial.modules.asr;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import opendial.arch.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class CFGRule {

	static Logger log = new Logger("CFGRule", Logger.Level.NORMAL);
	
	String lhs;
	
	List<String> rhs;
	
	public CFGRule(String lhs) {
		this.lhs = lhs;
		rhs = new LinkedList<String>();
	}
	
	public void addRightHandSide(String alternative) {
		rhs.add(alternative);
	}
	
	public String sampleRightHandSide() {
		Random rand = new Random();
		int selection = rand.nextInt(rhs.size());
		return rhs.get(selection);
	}

	/**
	 * 
	 * @return
	 */
	public String getLeftHandSide() {
		return lhs;
	}
	
	public List<String> getAllRightHandSides() {
		return rhs;
	}
	
	public String toString() {
		String str = "" ;
		for (String rh : rhs) {
			str += lhs + " ==> " + rh + "\n";
		}
		return str;
	}
}
