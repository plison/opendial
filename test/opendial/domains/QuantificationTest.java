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
import opendial.common.InferenceChecks;
import opendial.modules.core.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.state.StatePruner;

import org.junit.Test;

public class QuantificationTest {

	// logger
	public static Logger log = new Logger("QuantificationTest", Logger.Level.DEBUG);

	public static final String domainFile = "test//domains//domain5.xml";
	public static final String domainFile2 = "test//domains//domainthesis.xml";

	static InferenceChecks inference;
	static DialogueSystem system;



	@Test
	public void test1() throws DialException {

			Domain domain = XMLDomainReader.extractDomain(domainFile); 
			inference = new InferenceChecks();
			inference.EXACT_THRESHOLD = 0.06;
			system = new DialogueSystem(domain);
			system.getSettings().showGUI = false;
			system.detachModule(ForwardPlanner.class);
			StatePruner.ENABLE_PRUNING = false;
			system.startSystem(); 
		

		inference.checkProb(system.getState(), "found", "A", 0.7);

		inference.checkProb(system.getState(), "found2", "D", 0.3);
		inference.checkProb(system.getState(), "found2", "C", 0.5);
		
		StatePruner.ENABLE_PRUNING = true;
}
	

	@Test
	public void test2() throws DialException {
		inference = new InferenceChecks();

		Domain domain = XMLDomainReader.extractDomain(domainFile2); 
		system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.detachModule(ForwardPlanner.class);
		StatePruner.ENABLE_PRUNING = false;
		system.startSystem(); 
		inference.checkProb(system.getState(), "graspable(obj1)", "true", 0.81);

		inference.checkProb(system.getState(), "graspable(obj2)", "true", 0.16);
		inference.checkUtil(system.getState(), "a_m'", "grasp(obj1)", 0.592);
		inference.checkUtil(system.getState(), "a_m'", "grasp(obj2)", -2.0);
		inference.checkUtil(system.getState(), "a_m'", "grasp(obj3)", -2.0);
		
		StatePruner.ENABLE_PRUNING = true;
		
	}	
	


}

