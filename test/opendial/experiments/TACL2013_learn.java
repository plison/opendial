package opendial.experiments;
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


import java.util.List;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.EqualityDistribution;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.common.InferenceChecks;
import opendial.domains.Domain;
import opendial.gui.GUIFrame;
import opendial.gui.WOZFrame;
import opendial.inference.sampling.WoZQuerySampling;
import opendial.modules.NaoBehaviour;
import opendial.modules.NaoPerception;
import opendial.modules.NaoTTS;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLSettingsReader;
import opendial.readers.XMLStateReader;
import opendial.readers.XMLTrainingDataReader;
import opendial.simulation.UserSimulator;
import opendial.simulation.WozLearnerSimulator;
import opendial.simulation.datastructs.WoZDataPoint;
import opendial.state.rules.AnchoredRuleCache;

public class TACL2013_learn {

	// logger
	public static Logger log = new Logger("Main", Logger.Level.DEBUG);

	public static final String settingsFile = "domains//tacl2013/settings_learn.xml";

//	public static final String domainFile = "domains//tacl2013/domain_structured.xml";
//	public static final String parameterFile = "domains//tacl2013/params_structured.xml";
//	public static final String suffix = "structured";

	public static void main(String[] args) {
		try {
			if (args.length < 3) {
				throw new DialException("must provide arguments for domain, parameter and suffix for results");
			}
			String domainFile = args[0];
			String parametersFile = args[1];
			String suffix = args[2];
			
			
			Domain domain = XMLDomainReader.extractDomain(domainFile);
			Settings settings = XMLSettingsReader.extractSettings(settingsFile); 
		
			Settings.getInstance().nbSamples = (args.length > 3)? Integer.parseInt(args[3]) : Settings.getInstance().nbSamples;
			WoZQuerySampling.RATE = (args.length > 4)? Integer.parseInt(args[4]) : WoZQuerySampling.RATE;
			WoZQuerySampling.MIN = (args.length > 5)? Integer.parseInt(args[5]) : WoZQuerySampling.MIN;
			WoZQuerySampling.MAX = (args.length > 6)? Integer.parseInt(args[6]) : WoZQuerySampling.MAX;
			WoZQuerySampling.NONE_FACTOR = (args.length > 7)? Double.parseDouble(args[7]) : WoZQuerySampling.NONE_FACTOR;
			EqualityDistribution.PROB_WITH_SINGLE_NONE = (args.length > 8)? Double.parseDouble (args[8]) : EqualityDistribution.PROB_WITH_SINGLE_NONE;
			AnchoredRuleCache.PROB_THRESHOLD = (args.length >9)? Double.parseDouble(args[9]): AnchoredRuleCache.PROB_THRESHOLD;
			SimpleEmpiricalDistribution.USE_KDE = (args.length > 10)? Boolean.parseBoolean(args[10]) : SimpleEmpiricalDistribution.USE_KDE;
			
			DialogueSystem system = new DialogueSystem(settings, domain);

		BNetwork params = XMLStateReader.extractBayesianNetwork(parametersFile);
		system.addParameters(params);
		
		List<WoZDataPoint> data = XMLTrainingDataReader.
				extractTrainingSample(settings.planning.getWoZFile());
		
		log.debug("number of collected points: " + data.size());
		WozLearnerSimulator simulator = new WozLearnerSimulator(system.getState(), data);
		system.attachSimulator(simulator);
		simulator.specifyOutput(domainFile, suffix);
		

		if (!settings.planning.getWoZTestFile().equals("")) {
		List<WoZDataPoint> test = XMLTrainingDataReader.
				extractTrainingSample(settings.planning.getWoZTestFile());
		simulator.setTestData(test);
		}

		system.startSystem(); 
		
		}
		catch (DialException e) {
			log.warning("exception thrown " + e + ", aborting");
		}
	}
	
}

