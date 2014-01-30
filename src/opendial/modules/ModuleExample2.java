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


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;

/**
 * Simple example of an asynchronous module for the domain specified in 
 * domains/examples/example-step-by-step.xml.
 * 
 * <p>The example creates a small control window where the user can click to
 * provide directions to the agent.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ModuleExample2 implements Module {

	// logger
	public static Logger log = new Logger("ModuleExample2", Logger.Level.DEBUG);

	DialogueSystem system;
	JFrame frame;
	boolean paused = true;
	
	
	/**
	 * Creates the example module.  The module must have access to the
	 * dialogue system since it will periodically write new content to it. 
	 * 
	 * @param system the dialogue system
	 */
	public ModuleExample2(DialogueSystem system) {
		this.system = system;
	}
	
	
	/**
	 * Creates a small control window with 4 arrow buttons.  When clicked, each
	 * button will create a new dialogue act corresponding to the instruction to
	 * perform, and add it to the dialogue state.
	 */
	@Override
	public void start() throws DialException {
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
		frame.setLocation(600,600);
		paused = false;
		frame.setVisible(true);
	}

	
	
	/**
	 * Does nothing.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {	}
	
	

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
			try {
			switch (((BasicArrowButton)arg0.getSource()).getDirection()) {
			case SwingConstants.NORTH : system.addContent(new Assignment("a_u", "Request(Forward)")); break;
			case SwingConstants.SOUTH : system.addContent(new Assignment("a_u", "Request(Backward)")); break;
			case SwingConstants.WEST : system.addContent(new Assignment("a_u", "Request(Left)")); break;
			case SwingConstants.EAST : system.addContent(new Assignment("a_u", "Request(Right)")); break;
			}
			}
			catch (DialException e) {
				log.warning("could not send instruction: " + e);
			}
		}
	}

}

