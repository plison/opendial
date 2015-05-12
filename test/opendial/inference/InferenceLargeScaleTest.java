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

import java.util.logging.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import opendial.bn.BNetwork;
import opendial.bn.values.Value;
import opendial.common.InferenceChecks;
import opendial.common.NetworkExamples;
import opendial.datastructs.Assignment;
import opendial.inference.exact.NaiveInference;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class InferenceLargeScaleTest {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	public static double PERCENT_COMPARISONS = 0.5;

	@Test
	public void testNetwork() {

		InferenceChecks inference = new InferenceChecks();
		inference.includeNaive(true);

		BNetwork bn = NetworkExamples.constructBasicNetwork2();

		Set<Set<String>> queryVarsPowerset = generatePowerset(bn.getChanceNodeIds());
		List<Assignment> evidencePowerset = generateEvidencePowerset(bn);

		int nbErrors = 0;
		for (Set<String> queryVars : queryVarsPowerset) {
			if (!queryVars.isEmpty()) {
				for (Assignment evidence : evidencePowerset) {

					if ((new Random()).nextDouble() < PERCENT_COMPARISONS / 100.0) {

						try {
							inference.checkProb(bn, queryVars, evidence);
						}
						catch (AssertionError e) {
							nbErrors++;
							if (nbErrors > 2) {
								throw new AssertionError(
										"more than 2 sampling errors");
							}
						}
					}
				}
			}
		}
		inference.showPerformance();
	}

	private static <T> Set<Set<T>> generatePowerset(Set<T> fullSet) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		if (fullSet.isEmpty()) {
			sets.add(new HashSet<T>());
			return sets;
		}
		List<T> list = new ArrayList<T>(fullSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
		for (Set<T> set : generatePowerset(rest)) {
			Set<T> newSet = new HashSet<T>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	private List<Assignment> generateEvidencePowerset(BNetwork bn) {
		List<Assignment> allAssignments = new LinkedList<Assignment>();

		Map<Assignment, Double> fullJoint = NaiveInference.getFullJoint(bn, false);
		for (Assignment a : fullJoint.keySet()) {
			Set<Set<Entry<String, Value>>> partialAssigns =
					generatePowerset(a.getEntrySet());
			for (Set<Entry<String, Value>> partial : partialAssigns) {
				Assignment p = new Assignment(partial);
				allAssignments.add(p);
			}
		}
		return allAssignments;
	}

}
