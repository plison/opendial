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
import java.util.Map;

import org.junit.Test;

import opendial.arch.DialConstants;
import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Type;
import opendial.domains.values.RangeValue;
import opendial.gui.NetworkVisualisation;
import opendial.inference.algorithms.NaiveInference;
import opendial.inference.algorithms.VariableElimination;
import opendial.inference.bn.Assignment;
import opendial.inference.bn.BNetwork;
import opendial.inference.converters.StateConverter;
import opendial.inference.distribs.Distribution;
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
		assertEquals("robot1", bn.getSortedNodes().get(1).getId());
		assertEquals("Exists(robot1)", bn.getSortedNodes().get(2).getId());	
		assertEquals("floor", bn.getSortedNodes().get(3).getId());
		
		Distribution query1 = VariableElimination.query(bn, Arrays.asList("name(robot1)"), new Assignment());
		assertEquals(0.9f, query1.getProb(new Assignment("name(robot1)", "Lenny")), 0.001f);
		Distribution query2 = VariableElimination.query(bn, Arrays.asList("floor"), new Assignment());
		assertEquals(1.0f, query2.getProb(new Assignment("floor", "init")), 0.001f);
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
			
		assertEquals(6, bn.getNodes().size());
		assertEquals("robot2", bn.getSortedNodes().get(0).getId());
		assertEquals("name(robot2)", bn.getSortedNodes().get(1).getId());
		assertEquals("feat(bla)", bn.getSortedNodes().get(2).getId());		
		assertEquals("Exists(robot2)", bn.getSortedNodes().get(3).getId());
		assertEquals("Exists(robot2)", bn.getSortedNodes().get(0).getInputNodes().get(0).getId());
		assertEquals("bla", bn.getSortedNodes().get(4).getId());
		assertEquals("bla", bn.getSortedNodes().get(2).getInputNodes().get(0).getId());
		assertEquals("floor", bn.getSortedNodes().get(5).getId());
		
		Distribution query1 = VariableElimination.query(bn, Arrays.asList("name(robot2)"), new Assignment());
		assertEquals(0.9f, query1.getProb(new Assignment("name(robot2)", "Lenny")), 0.001f);
		Distribution query2 = VariableElimination.query(bn, Arrays.asList("floor"), new Assignment());
		assertEquals(1.0f, query2.getProb(new Assignment("floor", "init")), 0.001f);
		Distribution query3 = VariableElimination.query(bn, Arrays.asList("bla"), new Assignment());
		assertEquals(0.8f, query3.getProb(new Assignment("bla", "blaval1")), 0.001f);
		Distribution query4 = VariableElimination.query(bn, Arrays.asList("feat(bla)"), new Assignment());
		assertEquals(0.8f*0.8f, query4.getProb(new Assignment("feat(bla)", "36")), 0.001f);
		
	}
	
	
	@Test
	public void entityExtraction3() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		
		StateConverter converter = new StateConverter();
		DialogueState state = domain.getInitialState();
		state.getFluents().get(1).setExistenceProb(0.9f);
		
		Type type = new Type("type1");
		type.setAsFixed(true);
		type.addValues(Arrays.asList("val1_type1", "val2_type1"));
		
		Type ftype1 = new Type("feat1");
		ftype1.addValues(Arrays.asList("val1_feat1", "val2_feat1"));
		type.addPartialFeature(ftype1, "val1_type1");
		
		Type ftype2 = new Type("feat2");
		ftype2.addValues(Arrays.asList("val1_feat2", "val2_feat2"));
		ftype1.addPartialFeature(ftype2, "val2_feat1");

		Type ftype3 = new Type("feat3");
		ftype3.addValues(Arrays.asList("val1_feat3", "val2_feat3"));
		ftype1.addFullFeature(ftype3);


		Fluent newFluent = new Fluent(type);
		newFluent.addValue("val1_type1", 0.8f);
		newFluent.addValue("val2_type1", 0.2f);

		Fluent fFluent1 = new Fluent(ftype1);
		fFluent1.addValue("val1_feat1", 0.4f);
		fFluent1.addValue("val2_feat1", 0.6f);
		newFluent.addFeature(fFluent1);
	
		Fluent fFluent2 = new Fluent(ftype2);
		fFluent2.addValue("val1_feat2", 0.8f);
		fFluent1.addFeature(fFluent2);
	
		Fluent fFluent3 = new Fluent(ftype3);
		fFluent3.addValue("val1_feat3", 0.7f);
		fFluent1.addFeature(fFluent3);
			
		state.addFluent(newFluent);		
	
		BNetwork bn = converter.buildBayesianNetwork(state);	
		
	//	NetworkVisualisation.showBayesianNetwork(bn);
	//	Thread.currentThread().sleep(30000);
		
		assertEquals(8, bn.getNodes().size());
		assertEquals("feat2(feat1(type1))", bn.getSortedNodes().get(0).getId());
		assertEquals("feat3(feat1(type1))", bn.getSortedNodes().get(1).getId());
		assertEquals("feat1(type1)", bn.getSortedNodes().get(2).getId());
		assertEquals("robot3", bn.getSortedNodes().get(3).getId());
		assertEquals("name(robot3)", bn.getSortedNodes().get(4).getId());
		assertEquals("Exists(robot3)", bn.getSortedNodes().get(5).getId());
		assertEquals("Exists(robot3)", bn.getSortedNodes().get(3).getInputNodes().get(0).getId());
		assertEquals("Exists(robot3)", bn.getSortedNodes().get(4).getInputNodes().get(0).getId());
		assertEquals("floor", bn.getSortedNodes().get(6).getId());
		assertEquals(1, bn.getSortedNodes().get(3).getInputNodes().size());
		assertEquals("type1", bn.getSortedNodes().get(7).getId());
		
		Distribution query1 = VariableElimination.query(bn, Arrays.asList("name(robot3)"), new Assignment());
		assertEquals(0.9f, query1.getProb(new Assignment("name(robot3)", "Lenny")), 0.001f);
		
		Distribution query2 = VariableElimination.query(bn, Arrays.asList("floor"), new Assignment());
		assertEquals(1.0f, query2.getProb(new Assignment("floor", "init")), 0.001f);
		Distribution query3 = VariableElimination.query(bn, Arrays.asList("type1"), new Assignment());
		assertEquals(0.8f, query3.getProb(new Assignment("type1", "val1_type1")), 0.001f);
		Distribution query4 = VariableElimination.query(bn, Arrays.asList("feat1(type1)"), new Assignment());
		assertEquals(0.8f*0.4f, query4.getProb(new Assignment("feat1(type1)", "val1_feat1")), 0.001f);
	
		Distribution query5 = VariableElimination.query(bn, Arrays.asList("feat2(feat1(type1))"), new Assignment());
		assertEquals(0.8f*0.6f*0.8f, query5.getProb(new Assignment("feat2(feat1(type1))", "val1_feat2")), 0.001f);
		
		
		Distribution query6 = VariableElimination.query(bn, Arrays.asList("feat3(feat1(type1))"), new Assignment());
		assertEquals(0.8f*0.7f, query6.getProb(new Assignment("feat3(feat1(type1))", "val1_feat3")), 0.001f);
	}
	
	
}
