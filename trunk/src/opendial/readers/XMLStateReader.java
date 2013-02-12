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

package opendial.readers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.distribs.continuous.MultivariateDistribution;
import opendial.bn.distribs.continuous.UnivariateDistribution;

import opendial.bn.distribs.continuous.functions.DirichletDensityFunction;
import opendial.bn.distribs.continuous.functions.UnivariateDensityFunction;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.bn.values.VectorVal;
import opendial.domains.Domain;
import opendial.utils.XMLUtils;

/**
 * XML reader for the initial state specification
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

		// getting the VarNodes explicitly declared
		List<ChanceNode> initVarNodes = getInitialVarNodes(mainNode);
		for (ChanceNode varNode : initVarNodes) {
			state.addNode(varNode);
		}
		return state;
	}



	/**
	 * Returns the list of VarNodes explicitly defined in the XML node
	 *  
	 * @param mainNode the XML node
	 * @return the list of declared VarNodes
	 * @throws DialException if XML document is ill-formatted
	 */
	public static List<ChanceNode> getInitialVarNodes(Node mainNode) throws DialException {

		List<ChanceNode> initVarNodes = new LinkedList<ChanceNode>();
		NodeList mainNodeList = mainNode.getChildNodes();

		for (int i = 0; i < mainNodeList.getLength(); i++) {
			Node node = mainNodeList.item(i);
			if (node.getNodeName().equals("variable") && node.hasAttributes()) {
				Set<ChanceNode> varNode = createVariableNodes(node);
				initVarNodes.addAll(varNode);
			}
		}
		return initVarNodes;
	}


	// ===================================
	//  INDIVIDUAL VarNodeS
	// ===================================

	/**
	 * 
	 * 
	 * @param <T>
	 * @param node
	 * @param type
	 * @return
	 * @throws DialException
	 */
	public static Set<ChanceNode> createVariableNodes (Node node) throws DialException {

		Set<ChanceNode> nodes = new HashSet<ChanceNode>();
		
		if (node.hasAttributes() && node.getAttributes().getNamedItem("id")!=null) {
			String label = node.getAttributes().getNamedItem("id").getNodeValue();
			ChanceNode variable = new ChanceNode(label);
			nodes.add(variable);
			
			for (int i = 0 ; i < node.getChildNodes().getLength() ; i++) {

				Node subnode = node.getChildNodes().item(i);

				if (subnode.getNodeName().equals("value")) {
					
					// extracting the value
					String value = subnode.getFirstChild().getNodeValue().trim();
					
					// extracting the probability
					float prob = XMLUtils.getProbability (subnode);
		
					variable.addProb(ValueFactory.create(value),prob);

				}
				
				else if (subnode.getNodeName().equals("distrib")) {
					
					if (subnode.getAttributes().getNamedItem("type")!=null) {
						String distribType = subnode.getAttributes().getNamedItem("type").getNodeValue().trim();
						
						if (distribType.equalsIgnoreCase("gaussian")) {
							UnivariateDistribution distrib = new UnivariateDistribution(label, getGaussian(subnode));
							variable.setDistrib(distrib);
						}
						
						else if (distribType.equalsIgnoreCase("uniform")) {
							UnivariateDistribution distrib = new UnivariateDistribution(label, getUniform(subnode));
							variable.setDistrib(distrib);
						}
						else if (distribType.equalsIgnoreCase("dirichlet")) {
							MultivariateDistribution distrib = new MultivariateDistribution(label, getDirichlet(subnode));
							variable.setDistrib(distrib);
						}
						else {
							throw new DialException("distribution is not recognised: " + distribType);
						}

					}
				}
				
			}
			
			nodes.addAll(addFullFeatures(node, label + "."));
			return nodes;
		}
		else {
			throw new DialException("variable id is mandatory");
		}
		
	}
	
	
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
			return new DirichletDensityFunction((new VectorVal(alphas)).getArray());
		}
		throw new DialException("Dirichlet must have at least one alpha count");
	}
	
	// ===================================
	//  FULL AND PARTIAL FEATURES
	// ===================================


	


	/**
	 * Adds the full features declared in the XML node to the VarNode (if any is defined)
	 * 
	 * @param <T> parameter type
	 * @param node XML node
	 * @param VarNode VarNode in which to add the features
	 * @throws DialException if the features are not valid
	 */
	private static Set<ChanceNode> addFullFeatures (Node node, String baseNodeVar) throws DialException {

		Set<ChanceNode> nodes = new HashSet<ChanceNode>();
		
		for (int i = 0 ; i < node.getChildNodes().getLength() ; i++) {

			Node subnode = node.getChildNodes().item(i);

			/** if (subnode.getNodeName().equals("feature") && subnode.hasAttributes()) {		
				nodes.addAll(createVariableNodes(subnode, baseNodeVar));			
			} */
		}
		return nodes;
	}


	/**
	 * Adds the partial features declared in the XML node to the VarNode, with the given
	 * base value
	 * 
	 * @param <T> parameter type
	 * @param node XML node
	 * @param VarNode VarNode in which to add the partial features
	 * @param value base value for the partial features
	 * @throws DialException if the features are not valid according to the VarNode type
	 */
	/** private static <T> void addPartialFeatures (Node node, ChanceNode baseVarNode, String value) throws DialException {

		for (int j = 0 ; j < node.getChildNodes().getLength() ; j++) {

			Node subnode = node.getChildNodes().item(j);

			if (subnode.getNodeName().equals("feature") && subnode.hasAttributes()) {	
				FeatureVarNode<?,T> featVarNode = FeatureVarNode.createFeatureVarNode(createVarNode(subnode,featType),baseVarNode);
				baseVarNode.addFeature(featVarNode);

			}
		}
	} */


	

}
