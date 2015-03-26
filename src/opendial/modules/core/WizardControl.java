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

package opendial.modules.core;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.state.nodes.UtilityRuleNode;


/**
 * Module employed in the "Wizard-of-Oz" interaction mode.  The module extracts
 * all possible actions available for the current dialogue state and displays this
 * list of actions on the right side of the GUI.  The Wizard must then select
 * the action to perform. 
 * 
 * <p>The module only works if the GUI is activated.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-16 17:34:31 #$
 */
public class WizardControl implements Module {

	// logger
	public static Logger log = new Logger("WizardControl", Logger.Level.DEBUG);

	DialogueSystem system;
	GUIFrame gui;
	
	
	/**
	 * Creates a new wizard control for the dialogue system.
	 * 
	 * @param system the dialogue system
	 * @throws DialException if the GUI is not activated
	 */
	public WizardControl(DialogueSystem system) throws DialException {
		this.system = system;
		
		if (system.getModule(GUIFrame.class) == null) {
			throw new DialException("could not create wizard control: no GUI");
		}
		else {
			gui = system.getModule(GUIFrame.class);
		}
	}
	
	
	/**
	 * Does nothing
	 */
	@Override
	public void start()  {	}

	/**
	 * Does nothing
	 */
	@Override
	public void pause(boolean shouldBePaused) { 	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isRunning() {  return true;	}
	
	
	
	/**
	 * Triggers the wizard control. The wizard control window is displayed whenever the dialogue
	 * state contains at least one action node. 
	 * 
	 * <p>There is an exception: if the action selection is straightforward and does not contain
	 * any parameters (i.e. there is only one possible action and its utility is well-defined), 
	 * the wizard control directly selects this action.  This exception is there to allow for the 
	 * NLG module to directly realise the system's communicative intention without the wizard 
	 * intervention, if there is no doubt about how to realise the utterance.
	 */
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
			displayWizardBox(action);
		}
		state.addToState(Assignment.createDefault(state.getActionNodeIds()).removePrimes());
		state.removeNodes(state.getActionNodeIds());
		state.removeNodes(state.getUtilityNodeIds());
		}
		catch (DialException e) {
			log.warning("could not apply wizard control: " + e);
		}
	}
	
	
	/**
	 * Displays the Wizard-of-Oz window with the possible action values specified in the
	 * action node.
	 * 
	 * @param actionNode the action node
	 * @throws DialException if the action values could not be extracted
	 */
	@SuppressWarnings("serial")
	private void displayWizardBox(ActionNode actionNode) throws DialException {
		
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
		button.addActionListener(new WizardBoxListener(listBox, actionNode.getId().replace("'", ""), system.getState().copy()));

		InputMap inputMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(enter, "ENTER");
		button.getActionMap().put("ENTER", new AbstractAction() {
			@Override
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
	
	
	
	/**
	 * Action listener for the Wizard-of-Oz selection box.
	 */
	class WizardBoxListener implements ActionListener {

		JList listBox;
		String actionVar;
		DialogueState copy;
		
		public WizardBoxListener(JList listBox, String actionVar, DialogueState copy) {
			this.listBox = listBox;
			this.actionVar = actionVar;
			this.copy = copy;
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
				Set<String> updatedParams = WizardLearner.learnFromWizardAction(copy, action.addPrimes());
				for (String param : updatedParams) {
					system.getState().getChanceNode(param).setDistrib(copy.getChanceNode(param).getDistrib());
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

