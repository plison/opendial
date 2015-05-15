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

import java.util.logging.*;

import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

import org.junit.Test;

public class LearningTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String domainFile = "test//domains//domain-is.xml";
	public static final String parametersFile = "test//domains/params-is.xml";

	@Test
	public void testIS2013() throws InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		BNetwork params =
				XMLStateReader.extractBayesianNetwork(parametersFile, "parameters");
		domain.setParameters(params);
		// Settings.guiSettings.showGUI = true;
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.detachModule(ForwardPlanner.class);
		Settings.nbSamples = Settings.nbSamples * 3;
		Settings.maxSamplingTime = Settings.maxSamplingTime * 10;
		system.startSystem();

		double[] initMean =
				system.getContent("theta_1").toContinuous().getFunction().getMean();

		CategoricalTable.Builder builder = new CategoricalTable.Builder("a_u");
		builder.addRow("Move(Left)", 1.0);
		builder.addRow("Move(Right)", 0.0);
		builder.addRow("None", 0.0);
		system.addContent(builder.build());
		system.getState().removeNodes(system.getState().getUtilityNodeIds());
		system.getState().removeNodes(system.getState().getActionNodeIds());

		double[] afterMean =
				system.getContent("theta_1").toContinuous().getFunction().getMean();

		assertTrue(afterMean[0] - initMean[0] > 0.04);
		assertTrue(afterMean[1] - initMean[1] < 0.04);
		assertTrue(afterMean[2] - initMean[2] < 0.04);
		assertTrue(afterMean[3] - initMean[3] < 0.04);
		assertTrue(afterMean[4] - initMean[4] < 0.04);
		assertTrue(afterMean[5] - initMean[5] < 0.04);
		assertTrue(afterMean[6] - initMean[6] < 0.04);
		assertTrue(afterMean[7] - initMean[7] < 0.04);

		Settings.nbSamples = Settings.nbSamples / 3;
		Settings.maxSamplingTime = Settings.maxSamplingTime / 10;
	}
}
