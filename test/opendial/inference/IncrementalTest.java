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
                                                                

package opendial.inference;


import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

public class IncrementalTest2 {

	// logger
	public static Logger log = new Logger("IncrementalTest2",
			Logger.Level.NORMAL);
	
	
	public static final String domainFile = "test//domains//incremental-domain.xml";

	
	@Test
	public void test1() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = true;
		system.getSettings().recording = Settings.Recording.ALL;
		system.startSystem();
		system.addContent(new Assignment("floor", "user"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "go")), 5000);
		CategoricalTable t = new CategoricalTable();
		t.addRow(new Assignment("u_u", "forward"), 0.7);
		t.addRow(new Assignment("u_u", "backward"), 0.2);
		system.incrementContent(t, 5000);
		system.addContent(new Assignment("floor", "free"));
		system.incrementContent(new CategoricalTable(new Assignment("u_u", "please")), 500);
		
	}

}

