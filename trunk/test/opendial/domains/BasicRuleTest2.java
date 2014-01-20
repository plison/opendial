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

import java.util.Arrays;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.common.InferenceChecks;
import opendial.datastructs.Assignment;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.UtilQuery;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.StatePruner;
import opendial.state.distribs.EquivalenceDistribution;

import org.junit.Test;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class BasicRuleTest2 {

	// logger
	public static Logger log = new Logger("BasicRuleTest2", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain2.xml";
	public static final String domainFile2 = "test//domains//domain3.xml";
	public static final String domainFile3 = "test//domains//domain4.xml";

	static Domain domain;

	static InferenceChecks inference;

	static {
		try { 
			domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Test
	public void test() throws DialException, InterruptedException {
		
		DialogueSystem system = new DialogueSystem(domain);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;
		
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		system.startSystem(); 
		
		ProbQuery query = new ProbQuery(system.getState(),"a_u^p");
	 	inference.checkProb(query, new Assignment("a_u^p", "Ask(A)"), 0.63);
	 	inference.checkProb(query, new Assignment("a_u^p", "Ask(B)"), 0.27);
	 	inference.checkProb(query, new Assignment("a_u^p", "None"), 0.1);
	 			
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("a_u", "Ask(B)"), 0.8);
		table.addRow(new Assignment("a_u", "None"), 0.2); 

		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		
		system.addContent(table);

		ProbQuery query2 = new ProbQuery(system.getState(),"i_u");
	 	inference.checkProb(query2, new Assignment("i_u", "Want(A)"), 0.090);
	 	inference.checkProb(query2, new Assignment("i_u", "Want(B)"), 0.91);

	 	inference.checkProb(query, new Assignment("a_u^p", "Ask(B)"), 0.91*0.9);
	 	inference.checkProb(query, new Assignment("a_u^p", "Ask(A)"), 0.09*0.9);
	 	inference.checkProb(query, new Assignment("a_u^p", "None"), 0.1);

	 	ProbQuery query3 = new ProbQuery(system.getState(),"a_u");
	 	inference.checkProb(query3, new Assignment("a_u", "Ask(B)"), 0.918);
	 	inference.checkProb(query3, new Assignment("a_u", "None"), 0.081);
	 	
	 	EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;

}
	
	
	@Test
	public void test2() throws DialException, InterruptedException {
		 			
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;
		system.startSystem(); 

		ProbQuery query = new ProbQuery(system.getState(),"u_u2^p");
	 	inference.checkProb(query, new Assignment("u_u2^p", "Do A"), 0.216);
	 	inference.checkProb(query, new Assignment("u_u2^p", "Please do C"), 0.027);
	 	inference.checkProb(query, new Assignment("u_u2^p", "Could you do B?"), 0.054);
	 	inference.checkProb(query, new Assignment("u_u2^p", "Could you do A?"), 0.162);
	 	inference.checkProb(query, new Assignment("u_u2^p", "none"), 0.19);
	 			
	
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("u_u2", "Please do B"), 0.4);
		table.addRow(new Assignment("u_u2", "Do B"), 0.4); 
		 
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(table);
		
		query = new ProbQuery(system.getState(),"i_u2");
	 	inference.checkProb(query, new Assignment("i_u2", "Want(B)"), 0.654);
	 	inference.checkProb(query, new Assignment("i_u2", "Want(A)"), 0.1963);
	 	inference.checkProb(query, new Assignment("i_u2", "Want(C)"), 0.0327);
	 	inference.checkProb(query, new Assignment("i_u2", "none"), 0.1168);
	 	
	 	EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;
	}
	
	
	@Test
	public void test3() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		double eqFactor = EquivalenceDistribution.NONE_PROB;
		EquivalenceDistribution.NONE_PROB = 0.1;
		double oldPruneThreshold = StatePruner.VALUE_PRUNING_THRESHOLD;
		StatePruner.VALUE_PRUNING_THRESHOLD = 0.0;
		system.startSystem(); 

		UtilQuery query = new UtilQuery(system.getState(),"a_m'");
	 	inference.checkUtil(query, new Assignment("a_m'", "Do(A)"), 0.6);
	 	inference.checkUtil(query, new Assignment("a_m'", "Do(B)"), -2.6);
	 
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("a_u", "Ask(B)"), 0.8);
		table.addRow(new Assignment("a_u", "None"), 0.2); 
		system.getState().removeNodes(system.getState().getActionNodeIds());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.addContent(table);
		
		query = new UtilQuery(system.getState(),"a_m'");
	 	inference.checkUtil(query, new Assignment("a_m'", "Do(A)"), -4.35);
	 	inference.checkUtil(query, new Assignment("a_m'", "Do(B)"), 2.357);	

	 	EquivalenceDistribution.NONE_PROB = eqFactor;
		StatePruner.VALUE_PRUNING_THRESHOLD = oldPruneThreshold;
}
	
	

	@Test
	public void test4() throws DialException, InterruptedException {
		
		Domain domain2 = XMLDomainReader.extractDomain(domainFile2); 
		DialogueSystem system2 = new DialogueSystem(domain2);
		system2.getSettings().showGUI = false;
		system2.detachModule(ForwardPlanner.class);
		system2.startSystem(); 

		UtilQuery query = new UtilQuery(system2.getState(),Arrays.asList("a_m3'", "obj(a_m3)'"));
		
		inference.checkUtil(query, new Assignment(new Assignment("a_m3'", "Do"),
	 			new Assignment("obj(a_m3)'", "A")), 0.3);
	 	inference.checkUtil(query, new Assignment(new Assignment("a_m3'", "Do"),
	 			new Assignment("obj(a_m3)'", "B")), -1.7);
	 	inference.checkUtil(query, new Assignment(new Assignment("a_m3'", "SayHi"),
	 			new Assignment("obj(a_m3)'", "None")), -0.9);
	// 	assertEquals(5, (new LikelihoodWeighting()).queryUtil(query).getTable().size()); 
	 //	assertEquals(6, (new LikelihoodWeighting()).queryUtil(query).getTable().size()); 

}
	


	@Test
	public void test5() throws DialException, InterruptedException {
		
		Domain domain2 = XMLDomainReader.extractDomain(domainFile3); 
		DialogueSystem system2 = new DialogueSystem(domain2);
		system2.getSettings().showGUI = false;
		system2.detachModule(ForwardPlanner.class);
		system2.startSystem(); 

		UtilQuery query = new UtilQuery(system2.getState(),Arrays.asList("a_ml'", "a_mg'", "a_md'"));

	//	log.debug((new VariableElimination()).queryUtility(query));

		inference.checkUtil(query, new Assignment(new Assignment("a_ml'", "SayYes"),
	 			new Assignment("a_mg'", "Nod"), new Assignment("a_md'", "None")), 2.4);
		inference.checkUtil(query, new Assignment(new Assignment("a_ml'", "SayYes"),
	 			new Assignment("a_mg'", "Nod"), new Assignment("a_md'", "DanceAround")), -0.6);
		inference.checkUtil(query, new Assignment(new Assignment("a_ml'", "SayYes"),
	 			new Assignment("a_mg'", "None"), new Assignment("a_md'", "None")), 1.6);
	 	
	}
	
	

}
