// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.common.InferenceChecks;
import opendial.modules.core.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.StatePruner;

import org.junit.Test;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RuleTest1 {

	// logger
	public static Logger log = new Logger("RuleTest1", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain1.xml";

	static InferenceChecks inference;
	static Domain domain;

	static {
		try { 
			domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Test
	public void test1() throws DialException, InterruptedException {
	
		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
	
		inference.checkProb(system.getState(), "a_u", "Greeting", 0.8);
		inference.checkProb(system.getState(), "a_u", "None", 0.2);
	
		StatePruner.ENABLE_PRUNING = true;
	}


	@Test 
	public void test2() throws DialException {
				
		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
		
		inference.checkProb(system.getState(), "i_u", "Inform", 0.7*0.8);
		inference.checkProb(system.getState(), "i_u", "None", 1-0.7*0.8);
		
		StatePruner.ENABLE_PRUNING = true;
	}

	@Test
	public void test3() throws DialException, InterruptedException {

		inference.EXACT_THRESHOLD = 0.06;
		
		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
		inference.checkProb(system.getState(), "direction", "straight", 0.79);
		inference.checkProb(system.getState(), "direction", "left", 0.20);
		inference.checkProb(system.getState(), "direction", "right", 0.01);
		
		StatePruner.ENABLE_PRUNING = true;
	}


	@Test
	public void test4() throws DialException {
		
		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
		
		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.3);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1", 0.2);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3", 0.28);

		StatePruner.ENABLE_PRUNING = true;
	}


	@Test
	public void test5() throws DialException {
		
		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
			
		inference.checkProb(system.getState(), "o2", "here is value1", 0.35);
		inference.checkProb(system.getState(), "o2", "and value2 is over there", 0.07);
		inference.checkProb(system.getState(), "o2", "value3, finally", 0.28);
		
		StatePruner.ENABLE_PRUNING = true;

	}

	@Test
	public void test6() throws DialException, InterruptedException {

		inference.EXACT_THRESHOLD = 0.06;

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		system.getSettings().showGUI = false;
		system.startSystem(); 
		CategoricalTable table = new CategoricalTable("var1");
		table.addRow("value2", 0.9);
		system.addContent(table);
		
		inference.checkProb(system.getState(), "o", "and we have var1=value2", 0.9);
		inference.checkProb(system.getState(), "o", "and we have localvar=value1", 0.05);
		inference.checkProb(system.getState(), "o", "and we have localvar=value3", 0.04);
		
		StatePruner.ENABLE_PRUNING = true;
	}


	@Test
	public void test7() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(domain);
		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.getSettings().showGUI = false;
		system.startSystem(); 
		inference.checkProb(system.getState(), "a_u2", "[Greet, HowAreYou]", 0.7);
		inference.checkProb(system.getState(), "a_u2", "[]", 0.1);
		inference.checkProb(system.getState(), "a_u2", "[HowAreYou]", 0.2);
		
		StatePruner.ENABLE_PRUNING = true;
	}

	
}
