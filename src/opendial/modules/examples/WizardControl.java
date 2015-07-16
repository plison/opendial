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

package opendial.modules.examples;

import java.util.logging.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;

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

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.nodes.ActionNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.domains.rules.distribs.AnchoredRule;
import opendial.gui.GUIFrame;
import opendial.modules.ForwardPlanner;
import opendial.modules.Module;
import opendial.modules.WizardLearner;

/**
 * Module employed in the "Wizard-of-Oz" interaction mode. The module extracts all
 * possible actions available for the current dialogue state and displays this list
 * of actions on the right side of the GUI. The Wizard must then select the action to
 * perform.
 * 
 * <p>
 * The module only works if the GUI is activated.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class WizardControl implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;
	GUIFrame gui;

	/**
	 * Creates a new wizard control for the dialogue system.
	 * 
	 * @param system the dialogue system
	 */
	public WizardControl(DialogueSystem system) {
		this.system = system;

		if (system.getModule(GUIFrame.class) == null) {
			throw new RuntimeException("could not create wizard control: no GUI");
		}
		else {
			gui = system.getModule(GUIFrame.class);
		}
	}

	/**
	 * Does nothing
	 */
	@Override
	public void start() {
	}

	/**
	 * Does nothing
	 */
	@Override
	public void pause(boolean shouldBePaused) {
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean isRunning() {
		return true;
	}

	/**
	 * Triggers the wizard control. The wizard control window is displayed whenever
	 * the dialogue state contains at least one action node.
	 * 
	 * <p>
	 * There is an exception: if the action selection is straightforward and does not
	 * contain any parameters (i.e. there is only one possible action and its utility
	 * is well-defined), the wizard control directly selects this action. This
	 * exception is there to allow for the NLG module to directly realise the
	 * system's communicative intention without the wizard intervention, if there is
	 * no doubt about how to realise the utterance.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		// if the action selection is straightforward and parameter-less,
		// directly select the action
		if (state.getUtilityNodes().size() == 1) {
			UtilityNode urnode = state.getUtilityNodes().stream().findFirst().get();
			if (urnode.getFunction() instanceof AnchoredRule) {
				AnchoredRule arule = (AnchoredRule) urnode.getFunction();
				if (arule.getInputRange().linearise().size() == 1
						&& arule.getParameters().isEmpty()) {
					system.getModule(ForwardPlanner.class).trigger(state,
							updatedVars);
					return;
				}
			}

		}
		try {
			for (ActionNode action : state.getActionNodes()) {
				displayWizardBox(action);
			}
			state.addToState(Assignment.createDefault(state.getActionNodeIds())
					.removePrimes());
			state.removeNodes(state.getActionNodeIds());
			state.removeNodes(state.getUtilityNodeIds());
		}
		catch (RuntimeException e) {
			log.warning("could not apply wizard control: " + e);
		}
	}

	/**
	 * Displays the Wizard-of-Oz window with the possible action values specified in
	 * the action node.
	 * 
	 * @param actionNode the action node extracted
	 */
	@SuppressWarnings("serial")
	private void displayWizardBox(ActionNode actionNode) {

		DefaultListModel<String> model = new DefaultListModel<String>();
		for (Value v : actionNode.getValues()) {
			model.addElement(v.toString());
		}
		JList<String> listBox = new JList<String>(model);
		listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(200, 600));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Actions to select:"));

		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(scrollPane);
		final JButton button = new JButton("Select");
		DialogueState copy = system.getState().copy();
		button.addActionListener(e -> recordAction(copy, listBox,
				actionNode.getId().replace("'", "")));

		InputMap inputMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(enter, "ENTER");
		button.getActionMap().put("ENTER", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				button.doClick();
			}
		});

		container.add(button, BorderLayout.SOUTH);
		if (gui.getChatTab().getComponentCount() > 2) {
			gui.getChatTab().remove(gui.getChatTab().getComponent(2));
		}
		gui.getChatTab().add(container, BorderLayout.EAST);
		gui.getChatTab().repaint();
	}

	public void recordAction(DialogueState previousState, JList<String> listBox,
			String actionVar) {
		String actionValue = listBox.getModel()
				.getElementAt(listBox.getMinSelectionIndex()).toString();
		Assignment action = new Assignment(actionVar, actionValue);

		if (system.getModule(WizardLearner.class) != null) {
			system.getState().reset(previousState);
			system.getState().addEvidence(action.addPrimes());
			system.getModule(WizardLearner.class).trigger(system.getState(),
					Arrays.asList());
		}
		system.addContent(action);
		gui.getChatTab().remove(gui.getChatTab().getComponent(2));
	}

}
