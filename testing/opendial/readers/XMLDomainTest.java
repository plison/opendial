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
import opendial.domains.Model;
import opendial.domains.actions.VerbalAction;
import opendial.domains.observations.UtteranceObservation;
import opendial.domains.rules.Case;
import opendial.domains.rules.Rule;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.AssignEffect;
import opendial.domains.rules.variables.FeatureVariable;
import opendial.domains.rules.variables.FixedVariable;
import opendial.domains.types.ActionType;
import opendial.domains.types.EntityType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.FixedVariableType;
import opendial.domains.types.ObservationType;
import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.ComplexValue;
import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.state.StateEntity;
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
	public void entityExtraction() throws DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		log.debug("number of entity type: " + domain.getEntityTypes().size());
		assertEquals(2, domain.getEntityTypes().size());
		
		EntityType firstType = domain.getEntityType("intent");
		assertEquals("intent", firstType.getName());
		log.debug("number of values: " + firstType.getValues().size());
		assertEquals(2, firstType.getValues().size());
		assertEquals("WantX", firstType.getValues().get(0).getLabel());
		
		EntityType secondType = domain.getEntityType("robot");
		assertEquals("robot", secondType.getName());
		assertEquals(0, secondType.getValues().size());
		assertEquals(1, secondType.getFeatures().size());
		FeatureType featType = (FeatureType)secondType.getFeature("name");
		assertEquals(1, featType.getValues().size());
		assertTrue(featType.getValues().get(0) instanceof RangeValue);
		assertEquals("string", ((RangeValue)featType.getValues().get(0)).getRange());
		
	}
	
	@Test
	public void fixedVariableExtraction() throws DialException {

		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		FixedVariableType thirdType = domain.getFixedVariableType("a_u");
		assertEquals("a_u", thirdType.getName());
		assertEquals(4, thirdType.getValues().size());
		assertEquals(0, thirdType.getFeatures().size());
		Value complexVal = thirdType.getValues().get(1);
		assertTrue(complexVal instanceof ComplexValue);
		assertEquals("AskFor", ((ComplexValue)complexVal).getLabel());
		assertEquals(1, ((ComplexValue)complexVal).getFeatures().size());
		assertEquals(2, ((ComplexValue)complexVal).getFeatures().get(0).getValues().size());
		assertEquals("X", ((ComplexValue)complexVal).getFeatures().get(0).getValues().get(1).getLabel());
		
	}
	

	
	@Test
	public void observationExtraction() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		List<ObservationType> observations = domain.getObservationTypes();
		assertEquals(5, observations.size());
		ObservationType firstObservation = observations.get(0);
		assertTrue(firstObservation.getTrigger() instanceof UtteranceObservation);
		assertEquals("f1", firstObservation.getName());
		assertEquals("do X", ((UtteranceObservation)firstObservation.getTrigger()).getContent());
	}

	@Test
	public void actionExtraction() throws IOException, DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		List<ActionType> actions = domain.getActionTypes();
		assertEquals(1, actions.size());
		ActionType mainAction = actions.get(0);
		
		assertEquals(4, mainAction.getActionValues().size());
		log.debug(actions.size());

		assertTrue(mainAction.getActionValues().get(0) instanceof VerbalAction);
		assertEquals("AskRepeat", mainAction.getActionValues().get(0).getLabel());
		assertEquals("OK, doing X!", ((VerbalAction)mainAction.getActionValue("DoX")).getContent());
	}
	
	@Test
	public void initialStateExtraction() throws DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		
		assertEquals (1,domain.getInitialState().getVariables().size());
		StateEntity entity = domain.getInitialState().getVariables().get(0);
		log.debug("entity type: " + entity.getType().getName());
		assertEquals("robot", entity.getType().getName());
		log.info("label for state entity: " + entity.getLabel());
		assertTrue(entity.getValues().isEmpty());
		StateEntity feat = entity.getFeatures().get(0);
		assertEquals (1,feat.getValues().size());		
		assertEquals ("Lenny",feat.getValues().firstKey());		
		assertEquals (1.0f,feat.getValues().get("Lenny"), 0.01f);		
	}
	
	
	@Test
	public void modelExtraction1() throws DialException {
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		Model model = domain.getModel(Model.Type.USER_PREDICTION);
		
		assertEquals(2, model.getRules().size());
		Rule firstRule = model.getRules().get(0);
		
		assertTrue(firstRule.getInputVariables().isEmpty());
		assertEquals(1, firstRule.getOutputVariables().size());
		
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getDenotation());
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getType().getName());
		
		Case case1 = firstRule.getCases().get(0);
		
		assertTrue(case1.getCondition() instanceof VoidCondition);
		
		assertEquals(3, case1.getEffects().size());
		assertEquals(case1.getEffects().get(0).getProb(), 0.33f, 0.01f);
		assertTrue(case1.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_u", ((AssignEffect)case1.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("a_u", ((AssignEffect)case1.getEffects().get(0)).getVariable().getType().getName());
		assertEquals("AskForX", ((AssignEffect)case1.getEffects().get(0)).getValue());
		assertEquals("AskForY", ((AssignEffect)case1.getEffects().get(1)).getValue());
		
		Rule secondRule = model.getRules().get(1);
		assertEquals(2, secondRule.getInputVariables().size());
		assertEquals("i", secondRule.getInputVariable("i").getDenotation());
		assertEquals("intent", secondRule.getInputVariable("i").getType().getName());		
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
	public void modelExtraction2() throws DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		Model model = domain.getModel(Model.Type.SYSTEM_ACTIONVALUE);
		
		assertEquals(4, model.getRules().size());
		Rule firstRule = model.getRules().get(0);
		
		assertEquals(1, firstRule.getInputVariables().size());
		assertEquals("i", firstRule.getInputVariable("i").getDenotation());
		assertEquals(1, firstRule.getOutputVariables().size());
		assertEquals("a_m", firstRule.getOutputVariable("a_m").getDenotation());
		
		Case case1 = firstRule.getCases().get(0);
		assertTrue(case1.getCondition() instanceof BasicCondition);
		assertEquals("i", ((BasicCondition)case1.getCondition()).getVariable().getDenotation());
		assertEquals("WantX", ((BasicCondition)case1.getCondition()).getValue());
		
		assertEquals(1, case1.getEffects().size());
		assertEquals(case1.getEffects().get(0).getProb(), 1.0f, 0.01f);
		assertTrue(case1.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_m", ((AssignEffect)case1.getEffects().get(0)).getVariable().getDenotation());
		assertEquals("DoX", ((AssignEffect)case1.getEffects().get(0)).getValue());
		
		Case case2 = firstRule.getCases().get(1);
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
	public void modelExtraction3 () throws DialException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);

		Model model = domain.getModel(Model.Type.USER_REALISATION);
		
		Rule firstRule = model.getRules().get(0);
		
		assertEquals(2, firstRule.getInputVariables().size());
		assertEquals(1, firstRule.getOutputVariables().size());
		
		assertTrue(firstRule.getInputVariables().get(0) instanceof FixedVariable);
		assertTrue(firstRule.getInputVariables().get(1) instanceof FeatureVariable);
		assertEquals(((FeatureVariable)firstRule.getInputVariables().get(1)).getBaseVariable(), firstRule.getInputVariables().get(0));
	}
	
}
