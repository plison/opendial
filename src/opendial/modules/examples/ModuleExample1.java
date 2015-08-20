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
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;

/**
 * Simple example of a synchronous module for the domain specified in
 * domains/examples/example-step-by-step.xml.
 * 
 * <p>
 * The example creates a visual grid of size GRID_SIZE and updates the position of
 * the agent in accordance with the movements.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class ModuleExample1 implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static int GRID_SIZE = 11;

	JFrame frame;
	boolean paused = true;

	// start the agent in the middle of the grid
	int currentPosition = GRID_SIZE * GRID_SIZE / 2;

	public ModuleExample1(DialogueSystem system) {
	}

	/**
	 * Creates a simple visual grid of size GRID_SIZE and puts the agent in the
	 * middle of this grid.
	 */
	@Override
	public void start() {
		frame = new JFrame();

		frame.setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));
		for (int i = 0; i < GRID_SIZE; i++) {
			for (int j = 0; j < GRID_SIZE; j++) {
				JLabel label = new JLabel(" ");
				label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				frame.add(label);
			}
		}
		((JLabel) frame.getContentPane().getComponent(currentPosition))
				.setText(" HERE");
		frame.setSize(500, 500);
		frame.setVisible(true);
		paused = false;
	}

	/**
	 * If the updated variables contain the system action "a_m" and the action is a
	 * movement, updates the visual grid in accordance with the movement.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (updatedVars.contains("a_m") && state.hasChanceNode("a_m") && !paused) {
			String actionValue = state.queryProb("a_m").getBest().toString();
			if (actionValue.startsWith("Move(")) {
				String direction =
						actionValue.substring(5, actionValue.length() - 1);
				changePosition(direction);
			}
		}
	}

	/**
	 * Changes the position of the agent depending on the specified direction.
	 * 
	 * @param direction the direction, as a string.
	 */
	private void changePosition(String direction) {
		int newPosition = 0;
		if (direction.equals("Left")) {
			newPosition = currentPosition - 1;
		}
		else if (direction.equals("Right")) {
			newPosition = currentPosition + 1;
		}
		else if (direction.equals("Forward")) {
			newPosition = currentPosition - GRID_SIZE;
		}
		else if (direction.equals("Backward")) {
			newPosition = currentPosition + GRID_SIZE;
		}
		if (newPosition >= 0 && newPosition < GRID_SIZE * GRID_SIZE) {
			((JLabel) frame.getContentPane().getComponent(currentPosition))
					.setText(" ");
			currentPosition = newPosition;
			((JLabel) frame.getContentPane().getComponent(currentPosition))
					.setText(" HERE");
		}
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

}
