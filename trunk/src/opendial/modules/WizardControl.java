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

package opendial.modules;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.nodes.ActionNode;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.state.DialogueState;
import opendial.state.nodes.UtilityRuleNode;

public class WizardControl implements Module {

	// logger
	public static Logger log = new Logger("WizardControl", Logger.Level.DEBUG);

	DialogueSystem system;
	GUIFrame gui;
	
	
	public WizardControl(DialogueSystem system) throws DialException {
		this.system = system;
		
		if (system.getModule(GUIFrame.class) == null) {
			throw new DialException("could not create wizard control: no GUI");
		}
		else {
			gui = system.getModule(GUIFrame.class);
		}
	}
	
	public void start()  {	}


	public void pause(boolean shouldBePaused) { 	}

	public boolean isRunning() {  return true;	}
	
	
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		
		// if the action selection is straightforward and parameter-less, directly select the action
		if (state.getUtilityNodeIds().size() == 1 
				&& state.getUtilityNodes().iterator().next() instanceof UtilityRuleNode) {
			if (((UtilityRuleNode)state.getUtilityNodes().iterator().next()).getInputConditions().size() == 1) {
				system.getModule(ForwardPlanner.class).trigger(state, updatedVars);
			}
		}
		
		try {
				
		for (ActionNode action : state.getActionNodes()) {
			addActionSelection(action);
		}
		state.addToState(Assignment.createDefault(state.getActionNodeIds()).removePrimes());
		state.removeNodes(state.getActionNodeIds());
		state.removeNodes(state.getUtilityNodeIds());
		}
		catch (DialException e) {
			log.warning("could not apply wizard control: " + e);
		}
	}
	
	
	public void addActionSelection(ActionNode actionNode) {
		
		DefaultListModel model = new DefaultListModel();
		for (Value v : actionNode.getValues()) {
			model.addElement(v.toString());
		}
		JList listBox = new JList(model);
		listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());		
		scrollPane.setPreferredSize(new Dimension(200, 600));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Actions to select:"));

		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(scrollPane);
		final JButton button = new JButton("Select");
		button.addActionListener(new WizardBoxListener(listBox, actionNode.getId().replace("'", "")));

		InputMap inputMap = button.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(enter, "ENTER");
		button.getActionMap().put("ENTER", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				button.doClick();
		} });

		container.add(button, BorderLayout.SOUTH);
		if (gui.getChatTab().getComponentCount() > 2) {
			gui.getChatTab().remove(gui.getChatTab().getComponent(2));
		}
		gui.getChatTab().add(container, BorderLayout.EAST);
		gui.getChatTab().repaint();
	}
	
	
	
	
	class WizardBoxListener implements ActionListener {

		JList listBox;
		String actionVar;
		
		public WizardBoxListener(JList listBox, String actionVar) {
			this.listBox = listBox;
			this.actionVar = actionVar;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().getClass().equals(JButton.class)) {
				try {
				String actionValue = listBox.getModel().getElementAt(listBox.getMinSelectionIndex()).toString();
				Assignment action = new Assignment(actionVar, actionValue);
				if (system.getModule(DialogueRecorder.class) != null) {
					system.getModule(DialogueRecorder.class).addWizardAction(action);
				}
				system.addContent(action);
				gui.getChatTab().remove(gui.getChatTab().getComponent(2));
				}
				catch (DialException j) {
					log.warning("could not add wizard-selected action: " + j);
				}
			}
		}
		
	}


}

