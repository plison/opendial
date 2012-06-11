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

package opendial.inference;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.BNetworkStructureTest;
import opendial.bn.distribs.ProbDistribution;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BNetworkInferenceTest {

	// logger
	public static Logger log = new Logger("BNetworkInferenceTest",
			Logger.Level.NORMAL);
	
	
	@Test
	public void bayesianNetworkTest1() throws DialException {
		
		BNetwork bn = BNetworkStructureTest.constructBasicNetwork();
		Map<Assignment,Float> fullJoint = NaiveInference.getFullJoint(bn);

		
		assertEquals(0.000628f, fullJoint.get(new Assignment(
				Arrays.asList("JohnCalls", "MaryCalls", "Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		assertEquals(0.9367428f, fullJoint.get(new Assignment(
				Arrays.asList("!JohnCalls", "!MaryCalls", "!Alarm", "!Burglary", "!Earthquake"))), 0.000001f);
		
		ProbDistribution query = NaiveInference.query(bn, Arrays.asList("Burglary"), 
				new Assignment(Arrays.asList("JohnCalls", "MaryCalls")));
		
		assertEquals(0.7158281f, query.getProb(new Assignment(), new Assignment("Burglary", (false))), 0.0001f);
		assertEquals(0.28417188f, query.getProb(new Assignment(), new Assignment("Burglary", (true))), 0.0001f);
		

		ProbDistribution query2 = NaiveInference.query(bn, Arrays.asList("Alarm", "Burglary"), 
				new Assignment(Arrays.asList("Alarm", "MaryCalls")));
		
		assertEquals(0.62644875f, query2.getProb(new Assignment(), new Assignment(Arrays.asList("Alarm", "!Burglary"))), 0.001f);

	} 
}
