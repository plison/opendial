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

import opendial.arch.DialConstants.BinaryOperator;
import opendial.arch.DialConstants.ModelGroup;
import opendial.arch.DialConstants.PrimitiveType;
import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.Case;
import opendial.domains.rules.Rule;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.AssignEffect;
import opendial.domains.rules.effects.ComplexEffect;
import opendial.domains.rules.variables.FeatureVariable;
import opendial.domains.rules.variables.PointerVariable;
import opendial.domains.rules.variables.StandardVariable;
import opendial.domains.types.GenericType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.values.ActionValue;
import opendial.domains.types.values.BasicValue;
import opendial.domains.types.values.ObservationValue;
import opendial.domains.types.values.RangeValue;
import opendial.domains.types.values.Value;
import opendial.state.Fluent;
import opendial.utils.Logger;
import opendial.utils.XMLUtils;

import org.junit.Before;
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
	Domain domain;
	
	
	@Before
	public void openDomain() throws DialException {
		XMLDomainReader reader = new XMLDomainReader();
		domain = reader.extractDomain(dialDomain);
	}
	
	
	@Test
	public void validationTest() throws DialException {
		
		boolean isValidated = XMLUtils.validateXML(dialDomain, XMLDomainReader.domainSchema);
		assertTrue(isValidated);
	}
	
	
	@Test
	public void entityExtraction() throws DialException {
		
		log.debug("number of entity type: " + domain.getTypes().size());
	//	assertEquals(2, domain.getEntityTypes().size());
		
		GenericType firstType = domain.getType("intent");
		assertEquals("intent", firstType.getName());
		log.debug("number of values: " + firstType.getAllValues().size());
		assertEquals(1, firstType.getAllValues().size());
		assertEquals("Want", firstType.getBasicValues().get(0).getValue());
		assertEquals(1, firstType.getPartialFeatures("Want").size());
		assertEquals(2, firstType.getPartialFeatures("Want").get(0).getAllValues().size());
		
		GenericType secondType = domain.getType("robot");
		assertEquals("robot", secondType.getName());
		assertEquals(0, secondType.getAllValues().size());
		assertEquals(1, secondType.getFeatures().size());
		FeatureType featType = (FeatureType)secondType.getFeature("name");
		assertEquals(1, featType.getAllValues().size());
		assertTrue(featType.getAllValues().get(0) instanceof RangeValue);
		assertEquals(PrimitiveType.STRING, ((RangeValue)featType.getAllValues().get(0)).getRange());
		
	}
	 
	@Test
	public void fixedVariableExtraction() throws DialException {

		GenericType thirdType = domain.getType("a_u");
		assertEquals("a_u", thirdType.getName());
		assertEquals(4, thirdType.getAllValues().size());
		assertEquals(0, thirdType.getFullFeatures().size());
		Value val = thirdType.getValue("AskFor");
		assertEquals("AskFor", ((BasicValue<?>)val).getValue());
		assertEquals(1, thirdType.getPartialFeatures("AskFor").size());
		assertEquals(2, thirdType.getPartialFeatures("AskFor").get(0).getAllValues().size());
		assertEquals("X", ((BasicValue<?>) thirdType.getPartialFeatures("AskFor").get(0).getValue("X")).getValue());
		
	}
	

	
	@Test
	public void observationExtraction() throws IOException, DialException {

		// assertEquals(6, observations.size());
		GenericType firstObservation = domain.getType("doYObs");
		assertTrue(firstObservation.getAllValues().get(0) instanceof ObservationValue);
		assertEquals("doYObs", firstObservation.getName());
		assertEquals("do Y", ((ObservationValue<?>)firstObservation.getAllValues().get(0)).getTrigger().getContent());
	}

	@Test
	public void actionExtraction() throws IOException, DialException {

		GenericType mainAction = domain.getType("a_m");
		
		assertEquals(6, mainAction.getAllValues().size());

		assertTrue(mainAction.getValue("AskRepeat") instanceof ActionValue);
		assertEquals("AskRepeat", ((BasicValue<?>)mainAction.getValue("AskRepeat")).getValue());
		assertEquals("OK, doing X!", ((ActionValue<?>)mainAction.getValue("DoX")).getTemplate().getContent());
		assertEquals(1, ((ActionValue<?>)mainAction.getValue("SayHi")).getTemplate().getSlots().size());
		assertEquals("name", ((ActionValue<?>)mainAction.getValue("SayHi")).getTemplate().getSlots().get(0));
		assertTrue(mainAction.hasFeature("name"));
	}
	
	
	@Test
	public void initialStateExtraction() throws DialException {
		
		assertEquals (2,domain.getInitialState().getFluents().size());
		Fluent entity = domain.getInitialState().getFluents().get(0);
		log.debug("entity type: " + entity.getType().getName());
		
		assertEquals("robot", entity.getType().getName());
		log.info("label for state entity: " + entity.getLabel());
		assertTrue(entity.getValues().isEmpty());
		Fluent feat = entity.getFeatures().get(0);
		assertEquals (1,feat.getValues().size());		
		assertEquals ("Lenny",feat.getValues().firstKey());		
		assertEquals (1.0f,feat.getValues().get("Lenny"), 0.01f);
		
		Fluent variable = domain.getInitialState().getFluents().get(1);
		assertEquals("floor", variable.getType().getName());
		assertFalse(variable.getValues().isEmpty());
		assertEquals (1,variable.getValues().size());		
		assertEquals ("init",variable.getValues().firstKey());		
		assertEquals (1.0f,variable.getValues().get("init"), 0.01f);
	}
	
	
	@Test
	public void modelExtraction1() throws DialException {

		Model model = domain.getModel(ModelGroup.USER_PREDICTION);
		
		assertEquals(2, model.getRules().size());
		Rule firstRule = model.getRules().get(0);
		
		assertTrue(firstRule.getInputVariables().isEmpty());
		assertEquals(2, firstRule.getOutputVariables().size());
		
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getIdentifier());
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getType().getName());
		
		Case case1 = firstRule.getCases().get(0);
		
		assertTrue(case1.getCondition() instanceof VoidCondition);
		
		assertEquals(3, case1.getEffects().size());
		assertEquals(0.33f, case1.getProb(case1.getEffects().get(0)),  0.01f);
		assertTrue(((ComplexEffect)case1.getEffects().get(1)).getSubeffects().get(0) instanceof AssignEffect);
		assertEquals("a_u", ((AssignEffect<?>)((ComplexEffect)case1.getEffects().get(1)).getSubeffects().get(0)).getVariable().getIdentifier());
		assertEquals("a_u", ((AssignEffect<?>)((ComplexEffect)case1.getEffects().get(1)).getSubeffects().get(0)).getVariable().getType().getName());
		assertEquals("AskFor", ((AssignEffect<?>)((ComplexEffect)case1.getEffects().get(1)).getSubeffects().get(0)).getValue());
		assertEquals("AskFor", ((AssignEffect<?>)((ComplexEffect)case1.getEffects().get(1)).getSubeffects().get(0)).getValue());
		
		Rule secondRule = model.getRules().get(1);
		assertEquals(3, secondRule.getInputVariables().size());
		assertEquals("i", secondRule.getInputVariable("i").getIdentifier());
		assertEquals("intent", secondRule.getInputVariable("i").getType().getName());		
		assertEquals("a_m", secondRule.getInputVariable("a_m").getIdentifier());
		
		assertEquals("a_u", firstRule.getOutputVariable("a_u").getIdentifier());

		Iterator<Case> caseIt = secondRule.getCases().iterator();
		Case case2 = caseIt.next();
		
		assertTrue(case2.getCondition() instanceof ComplexCondition);
		assertEquals(3, ((ComplexCondition)case2.getCondition()).getSubconditions().size());
		assertTrue(((ComplexCondition)case2.getCondition()).getSubconditions().get(0) instanceof BasicCondition);
		assertEquals("i", ((BasicCondition<?>)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(0)).getVariable().getIdentifier());
		assertEquals("Want", ((BasicCondition<?>)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(0)).getValue());
		assertEquals("a_m", ((BasicCondition<?>)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(2)).getVariable().getIdentifier());
		assertEquals("AskRepeat", ((BasicCondition<?>)((ComplexCondition)case2.getCondition())
				.getSubconditions().get(2)).getValue());
		
		assertEquals(1, case2.getEffects().size());
		assertEquals(case2.getProb(case2.getEffects().get(0)), 1.0f, 0.01f);
		assertTrue(case2.getEffects().get(0) instanceof ComplexEffect);
		assertEquals("a_u", ((AssignEffect<?>)((ComplexEffect)case2.getEffects().get(0)).getSubeffects().get(0)).getVariable().getIdentifier());
		assertEquals("AskFor", ((AssignEffect<?>)((ComplexEffect)case2.getEffects().get(0)).getSubeffects().get(0)).getValue());
		
		Case case3 = caseIt.next();
		assertEquals("Want", ((BasicCondition<?>)((ComplexCondition)case3.getCondition())
				.getSubconditions().get(0)).getValue());
		assertEquals("AskFor", ((AssignEffect<?>)((ComplexEffect)case3.getEffects().get(0)).getSubeffects().get(0)).getValue());

	}
	
	 
	
	@Test
	public void modelExtraction2() throws DialException {

		Model model = domain.getModel(ModelGroup.SYSTEM_ACTIONVALUE);
		
		assertEquals(5, model.getRules().size());
		Rule firstRule = model.getRules().get(1);
		
		assertEquals(3, firstRule.getInputVariables().size());
		assertEquals("i", firstRule.getInputVariable("i").getIdentifier());
		assertEquals(1, firstRule.getOutputVariables().size());
		assertEquals("a_m", firstRule.getOutputVariable("a_m").getIdentifier());
		
		Case case1 = firstRule.getCases().get(0);
		assertTrue(case1.getCondition() instanceof ComplexCondition);
		assertEquals("i", ((BasicCondition<?>)((ComplexCondition)case1.getCondition()).getSubconditions().get(1)).getVariable().getIdentifier());
		assertEquals("Want", ((BasicCondition<?>)((ComplexCondition)case1.getCondition()).getSubconditions().get(1)).getValue());
		
		assertEquals(1, case1.getEffects().size());
		assertEquals(case1.getProb(case1.getEffects().get(0)), 1.0f, 0.01f);
		assertTrue(case1.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_m", ((AssignEffect<?>)case1.getEffects().get(0)).getVariable().getIdentifier());
		assertEquals("DoX", ((AssignEffect<?>)case1.getEffects().get(0)).getValue());
		
		Case case2 = firstRule.getCases().get(1);
		assertTrue(case2.getCondition() instanceof ComplexCondition);
		assertEquals("i", ((BasicCondition<?>)((ComplexCondition)case2.getCondition()).getSubconditions().get(1)).getVariable().getIdentifier());
		assertEquals("Want", ((BasicCondition<?>)((ComplexCondition)case2.getCondition()).getSubconditions().get(1)).getValue());

		assertEquals(1, case2.getEffects().size());
		assertEquals(case2.getProb(case2.getEffects().get(0)), 0.0f, 0.01f);
		assertTrue(case2.getEffects().get(0) instanceof AssignEffect);
		assertEquals("a_m", ((AssignEffect<?>)case2.getEffects().get(0)).getVariable().getIdentifier());
		assertEquals("DoX", ((AssignEffect<?>)case2.getEffects().get(0)).getValue());
	}
	
	
	@Test
	public void modelExtraction3 () throws DialException {

		Model model = domain.getModel(ModelGroup.USER_REALISATION);
		
		Rule firstRule = model.getRules().get(0);
		
		assertEquals(2, firstRule.getInputVariables().size());
		assertEquals(1, firstRule.getOutputVariables().size());
		
		assertTrue(firstRule.getInputVariables().get(0) instanceof StandardVariable);
		assertTrue(firstRule.getInputVariables().get(1) instanceof FeatureVariable);
		assertEquals(((FeatureVariable)firstRule.getInputVariables().get(1)).getBaseVariable(), firstRule.getInputVariables().get(0));
	}
	
	@Test
	public void modelExtraction4() throws DialException {

		Model model = domain.getModel(ModelGroup.SYSTEM_TRANSITION);
		
		assertEquals(2, model.getRules().size());
		
		Rule firstRule = model.getRules().get(0);
		
		assertEquals(2, firstRule.getInputVariables().size());
		assertEquals(1, firstRule.getOutputVariables().size());
		
		assertTrue(firstRule.getInputVariables().get(0) instanceof StandardVariable);
		assertTrue(firstRule.getInputVariables().get(1) instanceof StandardVariable);
		assertTrue(firstRule.getOutputVariables().get(0) instanceof PointerVariable);
		assertEquals(firstRule.getInputVariables().get(1), ((PointerVariable)firstRule.getOutputVariables().get(0)).getTarget());		
		
	}
	
	@Test 
	public void modelExtraction5() throws DialException {

		Model model = domain.getModel(ModelGroup.USER_TRANSITION);
		
		assertEquals(3, model.getRules().size());
		
		Rule firstRule = model.getRules().get(0);
		Condition firstCond = firstRule.getCases().get(0).getCondition();
		assertTrue(firstCond instanceof ComplexCondition);
		assertEquals(3, ((ComplexCondition)firstCond).getSubconditions().size());
		assertEquals(BinaryOperator.AND, ((ComplexCondition)firstCond).getBinaryOperator());
		Condition subCondition1 = ((ComplexCondition)firstCond).getSubconditions().get(0);
		assertTrue(subCondition1 instanceof BasicCondition);
		assertEquals("a_u", ((BasicCondition<?>)subCondition1).getVariable().getIdentifier());
		assertEquals("AskFor", ((BasicCondition<?>)subCondition1).getValue());		
	}
	
}
