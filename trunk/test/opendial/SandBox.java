package opendial;
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

import opendial.arch.Settings;
import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.ProbabilityRuleNode;
import opendial.common.NetworkExamples;
import opendial.domains.Domain;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.UpdateRule;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.gui.GUIFrame;
import opendial.gui.StateMonitorTab;
import opendial.gui.statemonitor.DistributionViewer;
import opendial.inference.ImportanceSampling;
import opendial.inference.NaiveInference;
import opendial.inference.VariableElimination;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.sampling.SampleCollector;
import opendial.readers.XMLDomainReader;
import opendial.state.DialogueState;
import opendial.state.rules.AnchoredRule;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SandBox {

	// logger
	public static Logger log = new Logger("SandBox", Logger.Level.DEBUG);
	
	public static final String domainFile = "domains//testing//domain1.xml";

	
	

	public static void main(String[] args) {
		ComplexCondition ccond = new ComplexCondition();
		BasicCondition cond1 = new BasicCondition("a_m", "Ground(*)", Relation.EQUAL);
		BasicCondition cond2 = new BasicCondition("a_m", "Ground({i_u})", Relation.UNEQUAL);
		ccond.addCondition(cond1);
		ccond.addCondition(cond2);
		log.debug("IS SATIF??" + ccond.isSatisfiedBy(new Assignment(new Assignment("i_u", "Move(Left)"), new Assignment("a_m", "Ground(Move(Left))"))));
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main22(String[] args) throws DialException, InterruptedException {
		UnivariateDistribution distrib = new UnivariateDistribution("x", new GaussianDensityFunction(0, 0.36));
		ChanceNode x = new ChanceNode("x");
		x.setDistrib(distrib);
		BNetwork network = new BNetwork();
		network.addNode(x);
		ProbDistribution distrib2 = (new ImportanceSampling()).queryProb(new ProbQuery(network, "x"));
		DistributionViewer.showDistributionViewer(distrib);
		Thread.sleep(3000000);
	}
	

	public static void main21(String[] args) throws DialException, InterruptedException {
		
		Pattern p = Pattern.compile("\\(([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,\\s*)*([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\)");
		Matcher m = p.matcher("(6)");
		log.debug("match " + m.matches());
		/**
		Domain domain = XMLDomainReader.extractDomain("domains//testing//basicPlanning.xml");
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem();
		Thread.sleep(300000000); */
	}
	

	public static void main11(String[] args) throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain("domains//testing//basicPlanning.xml");
		Settings.getInstance().activatePruning=false;
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem();
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("a_m", "AskRepeat"), 1.0);
		system.getState().addContent(table, "blabla");
//		Settings.getInstance().activatePlanner(true);
		Thread.sleep(300000000);
	}

	
	/**public static void main10(String[] args) throws DialException, InterruptedException {
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
	} */

	
	public static void main8(String[] args) throws DialException {
		log.info("");
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
