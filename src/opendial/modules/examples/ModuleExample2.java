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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;

/**
 * Simple example of an asynchronous module for the domain specified in
 * domains/examples/example-step-by-step.xml.
 * 
 * <p>
 * The example creates a small control window where the user can click to provide
 * directions to the agent.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class ModuleExample2 implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem system;
	JFrame frame;
	boolean paused = true;

	/**
	 * Creates the example module. The module must have access to the dialogue system
	 * since it will periodically write new content to it.
	 * 
	 * @param system the dialogue system
	 */
	public ModuleExample2(DialogueSystem system) {
		this.system = system;
	}

	/**
	 * Creates a small control window with 4 arrow buttons. When clicked, each button
	 * will create a new dialogue act corresponding to the instruction to perform,
	 * and add it to the dialogue state.
	 */
	@Override
	public void start() {
		frame = new JFrame();
		ActionListener listener = new CustomActionListener();
		frame.setLayout(new BorderLayout());

		JButton up = new BasicArrowButton(SwingConstants.NORTH);
		up.addActionListener(listener);
		frame.add(up, BorderLayout.NORTH);

		JButton west = new BasicArrowButton(SwingConstants.WEST);
		west.addActionListener(listener);
		frame.add(west, BorderLayout.WEST);

		frame.add(new JLabel("  "), BorderLayout.CENTER);

		JButton east = new BasicArrowButton(SwingConstants.EAST);
		east.addActionListener(listener);
		frame.add(east, BorderLayout.EAST);

		JButton south = new BasicArrowButton(SwingConstants.SOUTH);
		south.addActionListener(listener);
		frame.add(south, BorderLayout.SOUTH);

		frame.setSize(50, 100);
		frame.setLocation(600, 600);
		paused = false;
		frame.setVisible(true);
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
	}

	/**
	 * Pauses the module.
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true is the module is not paused, and false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

	/**
	 * Action listener for the arrow buttons.
	 */
	class CustomActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (paused) {
				return;
			}
			switch (((BasicArrowButton) arg0.getSource()).getDirection()) {
			case SwingConstants.NORTH:
				system.addContent("a_u", "Request(Forward)");
				break;
			case SwingConstants.SOUTH:
				system.addContent("a_u", "Request(Backward)");
				break;
			case SwingConstants.WEST:
				system.addContent("a_u", "Request(Left)");
				break;
			case SwingConstants.EAST:
				system.addContent("a_u", "Request(Right)");
				break;
			}
		}
	}

}
