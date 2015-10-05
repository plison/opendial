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

package opendial.readers;

import java.util.logging.*;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.bn.distribs.densityfunctions.DirichletDensityFunction;
import opendial.bn.distribs.densityfunctions.GaussianDensityFunction;
import opendial.bn.distribs.densityfunctions.UniformDensityFunction;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * XML reader for the initial state specification (and for parameters):
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLStateReader {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// ===================================
	// INITIAL STATE
	// ===================================

	/**
	 * Returns the initial state or parameters from the XML document, for the given
	 * domain (where the variable types are already declared)
	 * 
	 * @param file the file to process
	 * @param tag the XML tag to search for
	 * @return the specified Bayesian network
	 */
	public static BNetwork extractBayesianNetwork(String file, String tag) {

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(file);

		Node mainNode = XMLUtils.getMainNode(doc);

		if (mainNode.getNodeName().equals(tag)) {
			return getBayesianNetwork(mainNode);
		}
		for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
			Node childNode = mainNode.getChildNodes().item(i);
			if (childNode.getNodeName().equals(tag)) {
				return getBayesianNetwork(childNode);
			}
		}
		throw new RuntimeException("No tag " + tag + " found in file " + file);
	}

	/**
	 * Extracts the bayesian network from a XML string.
	 * 
	 * @param fullString the string containing the initial state content
	 * @return the corresponding Bayesian network
	 */
	public static BNetwork extractBayesianNetworkFromString(String fullString) {

		// extract the XML document
		InputSource is = new InputSource(new StringReader(fullString));
		Document doc = XMLUtils.getXMLDocument(is);

		Node mainNode = XMLUtils.getMainNode(doc);

		if (mainNode.getNodeName().equals("state")) {
			return getBayesianNetwork(mainNode);
		}
		for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
			Node childNode = mainNode.getChildNodes().item(i);
			if (childNode.getNodeName().equals("state")) {
				return getBayesianNetwork(childNode);
			}
		}
		log.warning("string does not contain 'xml' tag: " + fullString);
		return new BNetwork();
	}

	/**
	 * Returns the initial state or parameters from the XML document, for the given
	 * domain (where the variable types are already declared)
	 * 
	 * @param mainNode the main node for the XML document
	 * @return the corresponding dialogue state
	 */
	public static BNetwork getBayesianNetwork(Node mainNode) {

		BNetwork state = new BNetwork();

		for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
			Node node = mainNode.getChildNodes().item(i);
			if (node.getNodeName().equals("variable")) {
				ChanceNode chanceNode = createChanceNode(node);
				state.addNode(chanceNode);
			}
			else if (!node.getNodeName().equals("#text")
					&& !node.getNodeName().equals("#comment")) {
				throw new RuntimeException("Invalid tag in state: "
						+ node.getNodeName() + " not accepted");
			}

		}

		return state;
	}

	/**
	 * Creates a new chance node corresponding to the XML specification
	 * 
	 * @param node the XML node
	 * @return the resulting chance node encoded
	 */
	public static ChanceNode createChanceNode(Node node) {

		if (!node.hasAttributes() || node.getAttributes().getNamedItem("id") == null
				|| node.getAttributes().getNamedItem("id").getNodeValue().trim()
						.isEmpty()) {
			throw new RuntimeException("variable id is mandatory");
		}

		String label = node.getAttributes().getNamedItem("id").getNodeValue();

		CategoricalTable.Builder builder = new CategoricalTable.Builder(label);
		ContinuousDistribution distrib = null;

		for (int i = 0; i < node.getChildNodes().getLength(); i++) {

			Node subnode = node.getChildNodes().item(i);

			// first case: the chance node is described as a categorical table
			if (subnode.getNodeName().equals("value")) {

				// extracting the value
				Node child = subnode.getFirstChild();
				if (child != null) {
					// extracting the probability
					float prob = getProbability(subnode);
					Value v = ValueFactory.create(child.getNodeValue().trim());
					builder.addRow(v, prob);
				}
				else {
					throw new RuntimeException(
							"Value in variable " + label + " cannot be empty");
				}
			}

			// second case: the chance node is described by a parametric
			// continuous distribution
			else if (subnode.getNodeName().equals("distrib")) {

				if (subnode.getAttributes().getNamedItem("type") != null) {
					String distribType = subnode.getAttributes().getNamedItem("type")
							.getNodeValue().trim();

					if (distribType.equalsIgnoreCase("gaussian")) {
						distrib = new ContinuousDistribution(label,
								getGaussian(subnode));
					}

					else if (distribType.equalsIgnoreCase("uniform")) {
						distrib = new ContinuousDistribution(label,
								getUniform(subnode));
					}
					else if (distribType.equalsIgnoreCase("dirichlet")) {
						distrib = new ContinuousDistribution(label,
								getDirichlet(subnode));
					}
					else {
						throw new RuntimeException(
								"distribution is not recognised: " + distribType);
					}

				}
			}
		}

		if (distrib != null) {
			return new ChanceNode(label, distrib);
		}
		else {
			double total = builder.getTotalProb();
			if (total > 1.01) {
				throw new RuntimeException("Total probability is > 1: " + total);
			}
			return new ChanceNode(label, builder.build());
		}
	}

	/**
	 * Returns the probability of the value defined in the XML node (default to 1.0f
	 * is none is declared)
	 * 
	 * @param node the XML node
	 * @return the value probability
	 */
	private static float getProbability(Node node) {

		float prob = 1.0f;

		if (node.hasAttributes()
				&& node.getAttributes().getNamedItem("prob") != null) {
			String probStr =
					node.getAttributes().getNamedItem("prob").getNodeValue();

			try {
				prob = Float.parseFloat(probStr);
			}
			catch (NumberFormatException e) {
				XMLDomainReader.log.warning(
						"probability " + probStr + " not valid, assuming 1.0f");
			}
		}
		return prob;
	}

	/**
	 * Extracts the gaussian density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding Gaussian PDF properly encoded
	 */
	private static GaussianDensityFunction getGaussian(Node node) {
		double[] mean = null;
		double[] variance = null;
		for (int j = 0; j < node.getChildNodes().getLength(); j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("mean")) {
				String meanStr = subsubnode.getFirstChild().getNodeValue();
				if (meanStr.contains("[")) {
					mean = ((ArrayVal) ValueFactory.create(meanStr)).getArray();
				}
				else {
					mean = new double[] { Double.parseDouble(meanStr) };
				}
			}
			if (subsubnode.getNodeName().equals("variance")) {
				String varianceStr = subsubnode.getFirstChild().getNodeValue();
				if (varianceStr.contains("[")) {
					variance =
							((ArrayVal) ValueFactory.create(varianceStr)).getArray();
				}
				else {
					variance = new double[] { Double.parseDouble(varianceStr) };
				}
			}
		}
		if (mean != null && variance != null && mean.length == variance.length) {
			return new GaussianDensityFunction(mean, variance);
		}
		throw new RuntimeException("gaussian must specify both mean and variance");
	}

	/**
	 * Extracts the uniform density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding uniform PDF properly encoded
	 */
	private static UniformDensityFunction getUniform(Node node) {
		double min = Double.MAX_VALUE;
		double max = Double.MAX_VALUE;
		for (int j = 0; j < node.getChildNodes().getLength(); j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("min")) {
				min = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
			if (subsubnode.getNodeName().equals("max")) {
				max = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
		}
		if (min != Double.MAX_VALUE && max != Double.MAX_VALUE) {
			return new UniformDensityFunction(min, max);
		}
		throw new RuntimeException("uniform must specify both min and max");
	}

	/**
	 * Extracts the Dirichlet density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding Dirichlet PDF properly encoded
	 */
	private static DirichletDensityFunction getDirichlet(Node node) {
		List<Double> alphas = new LinkedList<Double>();
		for (int j = 0; j < node.getChildNodes().getLength(); j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("alpha")) {
				double alpha = Double
						.parseDouble(subsubnode.getFirstChild().getNodeValue());
				alphas.add(alpha);
			}
		}
		if (!alphas.isEmpty()) {
			return new DirichletDensityFunction((new ArrayVal(alphas)).getArray());
		}
		throw new RuntimeException("Dirichlet must have at least one alpha count");
	}

}
