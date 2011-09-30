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

package opendial.readers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.EntityType;
import opendial.domains.Model;
import opendial.domains.actions.Action;
import opendial.domains.actions.VerbalAction;
import opendial.domains.observations.Observation;
import opendial.domains.observations.StringObservation;
import opendial.domains.rules.Case;
import opendial.domains.rules.Rule;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.AssignEffect;
import opendial.utils.Logger;

import org.junit.Test;
 

/**
 *  Testing for the XML Reader of dialogue domains.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainTest {

	static Logger log = new Logger("XMLDomainTest", Logger.Level.DEBUG);

	
	public String dialDomain = "domains//testing//microdom2.xml";
	
	@Test
	public void validationTest() throws IOException {
		
		boolean isValidated = XMLDomainValidator.validateXML(dialDomain);
		assertTrue(isValidated);
	}
	
	
	@Test
	public void entityTypeExtraction() throws IOException, DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		assertEquals(3, domain.getEntityTypes().size());
		EntityType firstType = domain.getEntityType("intent");
		assertEquals("intent", firstType.getName());
		log.debug("number of values: " + firstType.getValues().size());
		assertEquals(2, firstType.getValues().size());
		assertEquals("WantX", firstType.getValues().get(0));
		
		// TODO: should also implement and test for features
	}
	
	
	@Test
	public void initialStateExtraction() throws IOException, DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		
		assertTrue (domain.getInitialState().getVariables().isEmpty());
	}
	
	
	@Test
	public void modelExtraction1() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		Model model = domain.getModel(Model.Type.USER_PREDICTION);
		
		assertEquals(2, model.getRules().size());
		Rule firstRule = model.getRules().get(0);
		
		assertTrue(firstRule.getInputVariables().isEmpty());
		assertEquals(1, firstRule.getOutputVariables().size());
		
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getDenotation());
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getType());
		
		Case case1 = firstRule.getCases().iterator().next();
		
		assertTrue(case1.getCondition() instanceof VoidCondition);
		
		assertEquals(3, case1.getEffects().size());
		assertEquals(case1.getEffects().get(0).getProb(), 0.33f, 0.01f);
		assertTrue(case1.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_u", ((AssignEffect)case1.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("a_u", ((AssignEffect)case1.getEffects().get(0)).getVariable().getType());
		assertEquals("AskForX", ((AssignEffect)case1.getEffects().get(0)).getValue());
		assertEquals("AskForY", ((AssignEffect)case1.getEffects().get(1)).getValue());
		
		Rule secondRule = model.getRules().get(1);
		assertEquals(2, secondRule.getInputVariables().size());
		assertEquals("i", secondRule.getInputVariable("i").getDenotation());
		assertEquals("intent", secondRule.getInputVariable("i").getType());		
		assertEquals("a_m", secondRule.getInputVariable("a_m").getDenotation());
		
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getDenotation());

		Iterator<Case> caseIt = secondRule.getCases().iterator();
		Case case2 = caseIt.next();
		
		assertTrue(case2.getCondition() instanceof ComplexCondition);
		assertEquals(2, ((ComplexCondition)case2.getCondition()).getSubconditions().size());
		assertTrue(((ComplexCondition)case2.getCondition()).getSubconditions().get(0) instanceof BasicCondition);
		assertEquals("i", ((BasicCondition)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(0)).getVariable().getDenotation());
		assertEquals("WantX", ((BasicCondition)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(0)).getValue());
		assertEquals("a_m", ((BasicCondition)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(1)).getVariable().getDenotation());
		assertEquals("AskRepeat", ((BasicCondition)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(1)).getValue());
		
		assertEquals(1, case2.getEffects().size());
		assertEquals(case2.getEffects().get(0).getProb(), 1.0f, 0.01f);
		assertTrue(case2.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_u", ((AssignEffect)case2.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("AskForX", ((AssignEffect)case2.getEffects().get(0)).getValue());
		
		Case case3 = caseIt.next();
		assertEquals("WantY", ((BasicCondition)((ComplexCondition)case3.getCondition())
				.getSubconditions().get(0)).getValue());
		assertEquals("AskForY", ((AssignEffect)case3.getEffects().get(0)).getValue());

	}
	
	
	
	@Test
	public void modelExtraction2() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		Model model = domain.getModel(Model.Type.SYSTEM_ACTIONVALUE);
		
		assertEquals(4, model.getRules().size());
		Rule firstRule = model.getRules().get(0);
		
		assertEquals(1, firstRule.getInputVariables().size());
		assertEquals("i", firstRule.getInputVariable("i").getDenotation());
		assertEquals(1, firstRule.getOutputVariables().size());
		assertEquals("a_m", firstRule.getOutputVariable("a_m").getDenotation());
		
		Iterator<Case> caseIt = firstRule.getCases().iterator();
		Case case1 = caseIt.next();
		
		assertTrue(case1.getCondition() instanceof BasicCondition);
		assertEquals("i", ((BasicCondition)case1.getCondition()).getVariable().getDenotation());
		assertEquals("WantX", ((BasicCondition)case1.getCondition()).getValue());
		
		assertEquals(1, case1.getEffects().size());
		assertEquals(case1.getEffects().get(0).getProb(), 1.0f, 0.01f);
		assertTrue(case1.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_m", ((AssignEffect)case1.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("DoX", ((AssignEffect)case1.getEffects().get(0)).getValue());
		
		Case case2 = caseIt.next();
		
		assertTrue(case2.getCondition() instanceof BasicCondition);
		assertEquals("i", ((BasicCondition)case2.getCondition()).getVariable().getDenotation());
		assertEquals("WantY", ((BasicCondition)case2.getCondition()).getValue());

		assertEquals(1, case2.getEffects().size());
		assertEquals(case2.getEffects().get(0).getProb(), 0.0f, 0.01f);
		assertTrue(case2.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_m", ((AssignEffect)case2.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("DoX", ((AssignEffect)case2.getEffects().get(0)).getValue());
	}
	
	
	
	@Test
	public void observationExtraction() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		List<Observation> observations = domain.getObservations();
		assertEquals(5, observations.size());
		assertTrue(observations.get(0) instanceof StringObservation);
		assertEquals("f1", observations.get(0).getName());
		assertEquals("do X", ((StringObservation)observations.get(0)).getContent());
	}

	@Test
	public void actionExtraction() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		List<Action> actions = domain.getActions();
		log.debug(actions.size());
		assertEquals(4, actions.size());
		assertTrue(actions.get(0) instanceof VerbalAction);
		assertEquals("DoX", actions.get(0).getValue());
		assertEquals("OK, doing X!", ((VerbalAction)actions.get(0)).getContent());
	}
}
