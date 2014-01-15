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

package opendial.readers;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.continuous.functions.DirichletDensityFunction;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.ValueFactory;
import opendial.utils.XMLUtils;

/**
 * XML reader for the initial state specification (and for parameters):
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-01-03 16:02:01 #$
 *
 */
public class XMLStateReader {

	// logger
	static Logger log = new Logger("XMLStateReader", Logger.Level.DEBUG);


	// ===================================
	//  INITIAL STATE
	// ===================================



	/**
	 * Returns the initial state or parameters from the XML document, for the given domain (where the
	 * variable types are already declared)
	 * 
	 * @param file the file to process
	 * @throws DialException if XML document is ill-formatted
	 */
	public static BNetwork extractBayesianNetwork(String file) throws DialException {

		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(file);

		Node mainNode = XMLUtils.getMainNode(doc);

		return getBayesianNetwork(mainNode);
	}



	/**
	 * Returns the initial state or parameters from the XML document, for the given domain (where the
	 * variable types are already declared)
	 * 
	 * @param doc the XML document
	 * @return the corresponding dialogue state
	 * @throws DialException if XML document is ill-formatted
	 */
	public static BNetwork getBayesianNetwork(Node mainNode) throws DialException {

		BNetwork state = new BNetwork();

		for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
			Node node = mainNode.getChildNodes().item(i);
			if (node.getNodeName().equals("variable") && node.hasAttributes()) {
				ChanceNode chanceNode = createChanceNode(node);
				state.addNode(chanceNode);
			}
		}

		return state;
	}

	/**
	 * Creates a new chance node corresponding to the XML specification
	 * 
	 * @param node the XML node
	 * @return the resulting chance node
	 * @throws DialException if the distribution is not properly encoded
	 */
	public static ChanceNode createChanceNode (Node node) throws DialException {

		if (!node.hasAttributes() || node.getAttributes().getNamedItem("id")==null) {
			throw new DialException("variable id is mandatory");
		}

		String label = node.getAttributes().getNamedItem("id").getNodeValue();
		ChanceNode variable = new ChanceNode(label);

		for (int i = 0 ; i < node.getChildNodes().getLength() ; i++) {

			Node subnode = node.getChildNodes().item(i);

			// first case: the chance node is described as a categorical table
			if (subnode.getNodeName().equals("value")) {

				// extracting the value
				String value = subnode.getFirstChild().getNodeValue().trim();

				// extracting the probability
				float prob = getProbability (subnode);

				variable.addProb(ValueFactory.create(value),prob);
			}

			// second case: the chance node is described by a parametric continuous distribution
			else if (subnode.getNodeName().equals("distrib")) {

				if (subnode.getAttributes().getNamedItem("type")!=null) {
					String distribType = subnode.getAttributes().getNamedItem("type").getNodeValue().trim();

					if (distribType.equalsIgnoreCase("gaussian")) {
						ContinuousDistribution distrib = new ContinuousDistribution(label, getGaussian(subnode));
						variable.setDistrib(distrib);
					}

					else if (distribType.equalsIgnoreCase("uniform")) {
						ContinuousDistribution distrib = new ContinuousDistribution(label, getUniform(subnode));
						variable.setDistrib(distrib);
					}
					else if (distribType.equalsIgnoreCase("dirichlet")) {
						ContinuousDistribution distrib = new ContinuousDistribution(label, getDirichlet(subnode));
						variable.setDistrib(distrib);
					}
					else {
						throw new DialException("distribution is not recognised: " + distribType);
					}

				}
			}
		}
		return variable;
	}



	/**
	 * Returns the probability of the value defined in the XML node
	 * (default to 1.0f is none is declared)
	 * 
	 * @param node the XML node
	 * @return the value probability
	 * @throws DialException if probability is ill-formatted
	 */
	private static float getProbability (Node node) {

		float prob = 1.0f;

		if (node.hasAttributes() && 
				node.getAttributes().getNamedItem("prob") != null) {
			String probStr = node.getAttributes().getNamedItem("prob").getNodeValue();

			try { prob = Float.parseFloat(probStr);	}
			catch (NumberFormatException e) {
				XMLDomainReader.log.warning("probability " + probStr +  " not valid, assuming 1.0f");
			}
		}
		return prob;
	}

	
	/**
	 * Extracts the gaussian density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding Gaussian PDF
	 * @throws DialException if the density function is not properly encoded
	 */
	private static GaussianDensityFunction getGaussian(Node node) throws DialException {
		double mean = Double.MAX_VALUE;
		double variance = Double.MAX_VALUE;
		for (int j = 0 ; j < node.getChildNodes().getLength() ; j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("mean")) {
				mean = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
			if (subsubnode.getNodeName().equals("variance")) {
				variance = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
		}
		if (mean!= Double.MAX_VALUE && variance != Double.MAX_VALUE) {
			return new GaussianDensityFunction(mean, variance);
		}
		throw new DialException("gaussian must specify both mean and variance");
	}


	/**
	 * Extracts the uniform density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding uniform PDF
	 * @throws DialException if the density function is not properly encoded
	 */
	private static UniformDensityFunction getUniform(Node node) throws DialException {
		double min = Double.MAX_VALUE;
		double max = Double.MAX_VALUE;
		for (int j = 0 ; j < node.getChildNodes().getLength() ; j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("min")) {
				min = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
			if (subsubnode.getNodeName().equals("max")) {
				max = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
			}
		}
		if (min!= Double.MAX_VALUE && max != Double.MAX_VALUE) {
			return new UniformDensityFunction(min, max);
		}
		throw new DialException("uniform must specify both min and max");
	}


	/**
	 * Extracts the Dirichlet density function described by the XML specification
	 * 
	 * @param node the XML node
	 * @return the corresponding Dirichlet PDF
	 * @throws DialException if the density function is not properly encoded
	 */
	private static DirichletDensityFunction getDirichlet(Node node) throws DialException {
		List<Double> alphas = new LinkedList<Double>();
		for (int j = 0 ; j < node.getChildNodes().getLength() ; j++) {
			Node subsubnode = node.getChildNodes().item(j);
			if (subsubnode.getNodeName().equals("alpha")) {
				double alpha = Double.parseDouble(subsubnode.getFirstChild().getNodeValue());
				alphas.add(alpha);
			}
		}
		if (!alphas.isEmpty()) {
			return new DirichletDensityFunction((new ArrayVal(alphas)).getArray());
		}
		throw new DialException("Dirichlet must have at least one alpha count");
	}


}