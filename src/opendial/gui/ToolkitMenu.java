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

package opendial.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.gui.statemonitor.options.InferenceOptionsPanel;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ToolkitMenu extends JMenuBar {

	// logger
	public static Logger log = new Logger("DialogueMonitorMenu",
			Logger.Level.DEBUG);
	
	public ToolkitMenu(final GUIFrame frame) {
		JMenu domainMenu = new JMenu("Domain");
		JMenuItem newDomain = new JMenuItem("New");
		JMenuItem openDomain = new JMenuItem("Open");
		JMenuItem editDomain = new JMenuItem("Edit");
		JMenuItem resetParams = new JMenuItem("Reset Parameters");
		JMenuItem saveParams = new JMenuItem("Save Parameters");
		JMenuItem saveParamsAs = new JMenuItem("Save Parameters As...");
		domainMenu.add(newDomain);
		domainMenu.add(openDomain);
		domainMenu.add(editDomain);
		domainMenu.add(new JSeparator());
		domainMenu.add(resetParams);
		domainMenu.add(saveParams);
		domainMenu.add(saveParamsAs);
		add(domainMenu);
		
		// NB: many of these action will only be available with a paused interaction
		JMenu editMenu = new JMenu("Edit");
		JMenu addMenu = new JMenu("Add New");
		JMenuItem chanceNode = new JMenuItem("Chance Node");
		JMenuItem valueNode = new JMenuItem("utility node");
		JMenuItem actionNode = new JMenuItem("Action Node");
		addMenu.add(chanceNode);
		addMenu.add(valueNode);
		addMenu.add(actionNode);
		editMenu.add(addMenu);
		JMenuItem editNode = new JMenuItem("Edit node");
		JMenuItem deleteNode = new JMenuItem("Delete node");
		JMenuItem drawDependency = new JMenuItem("Draw dependency link");
		JMenuItem marginalNode = new JMenuItem("Compute marginal distribution");
		JMenuItem graphNode = new JMenuItem("Show distribution graph");
		editMenu.add(editNode);
		editMenu.add(deleteNode);
		editMenu.add(drawDependency);
		editMenu.add(new JSeparator());
		editMenu.add(marginalNode);
		editMenu.add(graphNode);
		add(editMenu); 
		JMenu viewMenu = new JMenu("View");
		JMenuItem refreshItem = new JMenuItem("Refresh");
		viewMenu.add(refreshItem);
		JMenu showMenu = new JMenu("Show Tab");
		JMenuItem interactionWindowItem = new JMenuItem ("Interaction Window");
		JMenuItem stateMonitorItem = new JMenuItem ("Dialogue State Monitor");
		JMenuItem naoItem = new JMenuItem ("Nao Monitor"); // tab with 2 start button (ASR and Nao) and 2 log areas, connected to the scripts
		JMenuItem externalItem = new JMenuItem ("External Components Monitor");
		showMenu.add(interactionWindowItem);
		showMenu.add(stateMonitorItem);
		showMenu.add(naoItem);
		showMenu.add(externalItem);
		viewMenu.add(showMenu);
		viewMenu.add(new JSeparator());
		JMenu interactionMenu = new JMenu("User Utterances");
		JMenuItem singleBest = new JMenuItem("Single-best");
		JMenuItem threeBest = new JMenuItem("3-best list");
		JMenuItem allBest = new JMenuItem("Full N-best list");
		interactionMenu.add(singleBest);
		interactionMenu.add(threeBest);
		interactionMenu.add(allBest);
		viewMenu.add(interactionMenu);
		JMenu stateDisplayMenu = new JMenu("Dialogue State");
		JMenuItem reducedItem = new JMenuItem("Minimal");
		JMenuItem expandedItem = new JMenuItem("Full");
		stateDisplayMenu.add(reducedItem);
		stateDisplayMenu.add(expandedItem);
		add(viewMenu);
		viewMenu.add(stateDisplayMenu);
		JMenu traceMenu = new JMenu("Interaction");
		JMenuItem freezeItem = new JMenuItem("Pause/Resume");
		freezeItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().pause(!frame.getSystem().isPaused());
			}
		});
		JMenuItem resumeItem = new JMenuItem("Import Previous Trace");
		JMenuItem saveTraceItem = new JMenuItem("Save Trace As...");
		traceMenu.add(freezeItem);
		traceMenu.add(resumeItem);
		traceMenu.add(saveTraceItem);
		add(traceMenu);
		JMenu optionMenu = new JMenu("Options"); 
		JMenuItem asrItem = new JMenuItem("Speech Recognition"); // the language model / grammar should ideally be sent by opendial
		JMenuItem inferenceItem = new JMenuItem("Inference");
		JMenuItem simulatorItem = new JMenuItem ("Environment Simulator"); // if defined as such in the settings
		inferenceItem.getAccessibleContext().setAccessibleDescription(
        "Change parameters for the inference algorithm");
		inferenceItem.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				InferenceOptionsPanel inferencePanel = new InferenceOptionsPanel(frame);
				}
			});
		JMenuItem config = new JMenuItem("System Configuration");
		optionMenu.add(asrItem);
		optionMenu.add(inferenceItem);
		optionMenu.add(simulatorItem);
		optionMenu.add(new JSeparator());
		optionMenu.add(config);
		add(optionMenu);
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem("About");
		JMenuItem docItem = new JMenuItem("Documentation");
		helpMenu.add(aboutItem);
		helpMenu.add(docItem);
		add(helpMenu);
	}
}
