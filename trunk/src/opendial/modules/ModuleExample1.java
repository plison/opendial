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

package opendial.modules;


import java.awt.Color;
import java.awt.GridLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.state.DialogueState;

/**
 * Simple example of a synchronous module for the domain specified in 
 * domains/examples/example-step-by-step.xml.
 * 
 * <p>The example creates a visual grid of size GRID_SIZE and updates the
 * position of the agent in accordance with the movements.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ModuleExample1 implements Module {

	// logger
	public static Logger log = new Logger("ModuleExample1", Logger.Level.DEBUG);

	public static int GRID_SIZE = 11;

	JFrame frame;
	boolean paused = true;
	
	// start the agent in the middle of the grid
	int currentPosition = GRID_SIZE*GRID_SIZE/2;

	
	public ModuleExample1(DialogueSystem system) { }
	
	
	/**
	 * Creates a simple visual grid of size GRID_SIZE and puts the agent 
	 * in the middle of this grid.
	 */
	@Override
	public void start() throws DialException {
		frame = new JFrame();
		
		frame.setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));
		for (int i = 0 ; i < GRID_SIZE ; i++) {
			for (int j = 0 ; j < GRID_SIZE ; j++) {
				JLabel label = new JLabel(" ");
			    label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			    frame.add(label);
			}
		}
		((JLabel)frame.getContentPane().getComponent(currentPosition)).setText(" HERE");
		frame.setSize(500, 500);
		frame.setVisible(true);
		paused = false;
	}

	
	/**
	 * If the updated variables contain the system action "a_m" and the action is a movement,
	 * updates the visual grid in accordance with the movement.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (updatedVars.contains("a_m") && state.hasChanceNode("a_m") && !paused) {
			String actionValue = state.queryProb("a_m").toDiscrete().getBest().getValue("a_m").toString();
			if (actionValue.startsWith("Move(")) {
				String direction = actionValue.substring(5, actionValue.length()-1);
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
			newPosition = currentPosition -1;						
		}
		else if (direction.equals("Right")) {
			newPosition = currentPosition + 1;											
		}
		else if (direction.equals("Forward")) {
			newPosition = currentPosition -GRID_SIZE;											
		}
		else if (direction.equals("Backward")) {
			newPosition = currentPosition + GRID_SIZE;											
		}
		if (newPosition >= 0 && newPosition < GRID_SIZE*GRID_SIZE) {
			((JLabel)frame.getContentPane().getComponent(currentPosition)).setText(" ");	
			currentPosition = newPosition;
			((JLabel)frame.getContentPane().getComponent(currentPosition)).setText(" HERE");	
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

