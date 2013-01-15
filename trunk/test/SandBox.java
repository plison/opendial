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


import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.DialogueState;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.arch.statechange.AnchoredRule;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.common.NetworkExamples;
import opendial.domains.Domain;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.UpdateRule;
import opendial.gui.GUIFrame;
import opendial.gui.statemonitor.StateMonitorTab;
import opendial.inference.ImportanceSampling;
import opendial.inference.NaiveInference;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.sampling.SampleCollector;
import opendial.readers.XMLDomainReader;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SandBox {

	// logger
	public static Logger log = new Logger("SandBox", Logger.Level.NORMAL);
	
	public static final String domainFile = "domains//testing//domain1.xml";


	public static void main(String[] args) throws DialException, InterruptedException {
		SampleCollector.log.setLevel(Level.NORMAL);
		BNetwork network = NetworkExamples.constructBasicNetwork2();
		ReductionQuery redQuery = new ReductionQuery(network, "Burglary", "Earthquake", "MaryCalls");
		BNetwork reducedNet3 = (new ImportanceSampling(8000, 2000)).reduceNetwork(redQuery);
		ImportanceSampling is = new ImportanceSampling(8000, 2000);
		SampleCollector.log.setLevel(Level.DEBUG);

		ProbQuery query8 = new ProbQuery(reducedNet3, Arrays.asList("Earthquake"),
				new Assignment("!MaryCalls"));
		long t1 = System.nanoTime();
		is.queryProb(query8).toDiscrete().getProb(new Assignment(), new Assignment("Earthquake"));
		log.info("query time " + (System.nanoTime() - t1)/1000000000.0);
	}

	
	public static void main8(String[] args) throws DialException {
		log.info("");
	}

	
	public static void main6(String[] args) throws DialException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		DialogueState state = domain.getInitialState();
		
		GUIFrame tmf = GUIFrame.getSingletonInstance();
		try {
		tmf.updateCurrentState(state);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		state.applyRule(domain.getModels().get(0).getRules().iterator().next());
		
		try {
			tmf.updateCurrentState(state);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
	}

	
	public static void main3(String[] args) {
		GUIFrame tmf = GUIFrame.getSingletonInstance();
		try {
		tmf.updateCurrentState(new DialogueState(NetworkExamples.constructBasicNetwork4()));
		}
		catch (DialException e) {
			e.printStackTrace();
		}
	}
	
	public static void main2(String[] args) {
		String distribString = "PDF(blabla=(dd))=";
		Pattern p = Pattern.compile("PDF\\((.)*\\)=");
		Matcher m = p.matcher(distribString);
		while (m.find()) {
			String toreplace = m.group();
			distribString = distribString.replace(toreplace, toreplace.substring(0,toreplace.length()-2) + "|" + new Assignment("a", "b") + ")=");
		}
		log.info(distribString);
	}
}
