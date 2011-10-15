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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Type;
import opendial.domains.values.RangeValue;
import opendial.gui.NetworkVisualisation;
import opendial.inference.bn.BNetwork;
import opendial.inference.converters.StateConverter;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.state.Fluent;
import opendial.utils.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class StateConverterTest {

	static Logger log = new Logger("StateConverterTest", Logger.Level.DEBUG);
	
	public String dialDomain = "domains//testing//microdom2.xml";

	@Test
	public void entityExtraction1() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		
		StateConverter converter = new StateConverter();
		DialogueState state = domain.getInitialState();
		state.getFluents().get(1).setExistenceProb(0.9f);
		BNetwork bn = converter.buildBayesianNetwork(state);
		assertEquals(4, bn.getNodes().size());
		assertEquals("name(robot1)", bn.getSortedNodes().get(0).getId());
		assertEquals("floor", bn.getSortedNodes().get(1).getId());
		assertEquals("robot1", bn.getSortedNodes().get(2).getId());
		assertEquals("Exists(robot1)", bn.getSortedNodes().get(3).getId());
	//	NetworkVisualisation.showBayesianNetwork(bn);
	//	Thread.currentThread().sleep(10000);
	}
	
	
	@Test
	public void entityExtraction2() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		
		StateConverter converter = new StateConverter();
		DialogueState state = domain.getInitialState();
		state.getFluents().get(1).setExistenceProb(0.9f);
		
		Type type = new Type("bla");
		type.setAsFixed(true);
		type.addValues(Arrays.asList("blaval1", "blaval2"));
		Type ftype = new Type("feat");
		ftype.addValue(new RangeValue(DialConstants.PrimitiveType.INTEGER));
		type.addPartialFeature(ftype, "blaval1");
		
		Fluent newFluent = new Fluent(type);
		newFluent.addValue("blaval1", 0.8f);
		newFluent.addValue("blaval2", 0.2f);

		Fluent fFluent = new Fluent(ftype);
		fFluent.addValue("36", 0.8f);
		newFluent.addFeature(fFluent);
		
		state.addFluent(newFluent);		
	
		BNetwork bn = converter.buildBayesianNetwork(state);	
		
	//	NetworkVisualisation.showBayesianNetwork(bn);
	//	Thread.currentThread().sleep(10000);
	
		
		assertEquals(6, bn.getNodes().size());
		assertEquals("robot2", bn.getSortedNodes().get(0).getId());
		assertEquals("floor", bn.getSortedNodes().get(1).getId());
		assertEquals("name(robot2)", bn.getSortedNodes().get(2).getId());
		assertEquals("Exists(robot2)", bn.getSortedNodes().get(2).getInputNodes().get(0).getId());
		assertEquals("Exists(robot2)", bn.getSortedNodes().get(3).getId());
		assertEquals("feat(bla)", bn.getSortedNodes().get(4).getId());
		assertEquals("bla", bn.getSortedNodes().get(5).getId());
		assertEquals("bla", bn.getSortedNodes().get(4).getInputNodes().get(0).getId());

		
	}
}
