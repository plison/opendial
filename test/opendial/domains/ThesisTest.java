// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.domains;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.DirichletDensityFunction;
import opendial.bn.distribs.continuous.functions.KernelDensityFunction;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.ValueFactory;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.domains.rules.parameters.CompositeParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

public class ThesisTest {

	// logger
	public static Logger log = new Logger("ParametersTest", Logger.Level.DEBUG);
	
	public static final String domainFile = "test//domains//thesistest.xml";
	public static final String paramFile = "test//domains//thesisparams.xml";
	public static final String domainFile2 = "domains//demo//domain.xml";

	
	public void paramTest0() throws DialException, InterruptedException {
		Double[] d = new Double[2];
		d[0] = 3.0;
		d[1] = 1.0;
		DirichletDensityFunction f = new DirichletDensityFunction(d);
		int nb1 = 0 ;
		int nb2 = 0;
		for (int i = 0 ; i < 5000000 ; i++) {
			Double[] sample = f.sample();
			if (sample[0] > 0.74 && sample[0] < 0.78) {
	 			nb1++;
	 		}
	 		else if (sample[0] > 0.94 && sample[0] < 0.98) {
	 			nb2++;
	 		}
		}
	 	log.info("NB1 " + nb1);
	 	log.info("NB2: " + nb2);
	}
	
	
	// @Test
	public void paramTest1() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		BNetwork params = XMLStateReader.extractBayesianNetwork(paramFile);
		domain.setParameters(params);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.getSettings().enablePlan = false;
		Settings.nbSamples = 3000;
		system.getSettings().showGUI = false;
		Settings.maxSamplingTime = 800;
		
		system.startSystem();
	 	system.addContent(new Assignment("a_m", "AskRepeat"));
	 	
	 	CategoricalTable t = new CategoricalTable();
	 	t.addRow(new Assignment("a_u", "DoA"), 0.7);
	 	t.addRow(new Assignment("a_u", "DoC"), 0.2);
	 	t.addRow(new Assignment("a_u", "DoB"), 0.1);
		system.addContent(t);
		for (int i = 0 ; i < 3000 ; i++) {
			System.out.println(((ArrayVal)system.getState().getChanceNode("theta").sample()).getArray()[0]);
		}
	//	System.out.println("DENSITY: " + new ArrayVal(((ProductKernelDensityFunction)((MultivariateDistribution)system.getState().
	//			getNetwork().getChanceNode("theta").getDistrib().toContinuous()).getFunction()).getBandwidth()));
	}
	
	
	@Test
	public void demoTest() throws DialException {
		Domain domain = XMLDomainReader.extractDomain(domainFile2);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.startSystem();

		CategoricalTable t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "hello there"), 0.7);
	 	t.addRow(new Assignment("u_u", "hello"), 0.2);
		system.addContent(t);
		
		assertEquals("Hi there", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "move forward"), 0.06);
		system.addContent(t);

		assertEquals("Hi there", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "move forward"), 0.45);
		system.addContent(t);
		
		assertEquals("OK, moving Forward", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "now do that again"), 0.3);
	 	t.addRow(new Assignment("u_u", "move backward"), 0.22);
	 	t.addRow(new Assignment("u_u", "move a bit to the left"), 0.22);
		system.addContent(t);
		
		assertEquals("Sorry, could you repeat?", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "do that one more time"), 0.65);
		system.addContent(t);
		
		assertEquals("OK, moving Forward", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		system.addContent(new CategoricalTable(new Assignment("perceived", "[BlueObj]")));
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "what do you see"), 0.6);
	 	t.addRow(new Assignment("u_u", "do you see it"), 0.3);
		system.addContent(t);
		
		assertEquals("I see a blue cylinder", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "pick up the blue object"), 0.75);
	 	t.addRow(new Assignment("u_u", "turn left"), 0.12);
		system.addContent(t);
		
		assertEquals("OK, picking up the blue object", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		system.addContent(new CategoricalTable(new Assignment("perceived", "[]")));
		system.addContent(new CategoricalTable(new Assignment("carried", "[BlueObj]")));
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "now please move a bit forward"), 0.21);
	 	t.addRow(new Assignment("u_u", "move backward a little bit"), 0.13);
		system.addContent(t);
		
		assertEquals("Should I move a bit forward?", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "yes"), 0.8);
	 	t.addRow(new Assignment("u_u", "move backward"), 0.1);
		system.addContent(t);
		
		assertEquals("OK, moving Forward a little bit", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());

		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "and now move forward"), 0.21);
	 	t.addRow(new Assignment("u_u", "move backward"), 0.09);
		system.addContent(t);
		
		assertEquals("Should I move forward?", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());

		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "no"), 0.6);
		system.addContent(t);

		assertEquals("Should I move backward?", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());

		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "yes"), 0.5);
		system.addContent(t);
		
		assertEquals("OK, moving Backward", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "now what can you see now?"), 0.7);
		system.addContent(t);
		
		assertEquals("I do not see anything", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "please release the object"), 0.5);
		system.addContent(t);
		
		assertEquals("OK, putting down the object", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "something unexpected"), 0.7);
		system.addContent(t);
		
		assertEquals("OK, putting down the object", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());

		
		t = new CategoricalTable();
	 	t.addRow(new Assignment("u_u", "goodbye"), 0.7);
		system.addContent(t);
		
		assertEquals("Bye, see you next time", system.getContent("u_m").toDiscrete().getBest().getValue("u_m").toString());
		
	}
	
}

