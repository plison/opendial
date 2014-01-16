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

package opendial.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import opendial.DialogueSystem;
import opendial.arch.Logger;
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * Main GUI frame for the openDial toolkit, encompassing various tabs and
 * menus to control the application
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class GUIFrame implements Module {

	// logger
	public static Logger log = new Logger("GUIFrame", Logger.Level.DEBUG);

	
	JFrame frame;

	// tab for the state monitor
	StateViewerTab stateMonitorTab;
	
	// tab for the chat window
	ChatWindowTab chatTab;
	
	DialogueSystem system;		
	
	GUIMenuBar menu;
	
	public void start(DialogueSystem system) {

		this.system = system;

		if (system.getSettings().showGUI) {
		frame = new JFrame();

		// TODO: add " - domain name " when a domain is loaded
		frame.setTitle("OpenDial toolkit");
		
		JTabbedPane tabbedPane = new JTabbedPane();
		frame.getContentPane().add(tabbedPane);

		frame.setLocation(new Point(200, 200));

		frame.addWindowListener(new WindowAdapter() 
		{
			public void windowClosing(WindowEvent e) 
			{ System.exit(0); }
		}
		); 
		
		menu = new GUIMenuBar(this);
		frame.setJMenuBar(menu);
		
		chatTab = new ChatWindowTab(system);
		tabbedPane.addTab(ChatWindowTab.TAB_TITLE, null, chatTab, ChatWindowTab.TAB_TIP);

		stateMonitorTab = new StateViewerTab(this);
		tabbedPane.addTab(StateViewerTab.TAB_TITLE, null, stateMonitorTab, StateViewerTab.TAB_TIP);
				
		frame.setPreferredSize(new Dimension(1000,800));
		frame.pack();
		frame.setVisible(true);
		}
	}
	
	
	public void pause(boolean pause) {
		addComment((pause)? "system paused" : "system resumed");
		chatTab.updateActivation();
	}
	

	public DialogueSystem getSystem() {
		return system;
	}

	/**
	 * Updates the current dialogue state displayed in the component.  The current
	 * dialogue state is name "current" in the selection list.
	 * 
	 */
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (frame != null && frame.isVisible()) {
		chatTab.trigger(state, updatedVars);
		stateMonitorTab.trigger(state, updatedVars);
		menu.trigger();
			if (!frame.getTitle().equals(system.getDomain().getName())) {
				frame.setTitle("OpenDial toolkit - domain: " + system.getDomain().getName());
			}
		}
	}
	
	/**
	 * Records a dialogue state in the component and makes it available for display
	 * in the network selection list on the left side.  The network is associated with
	 * a specific name.  If the name already exists, the previous network is erased.
	 * 
	 * 
	 * @param state the dialogue state to record
	 * @param name the name for the recorded network
	 */
 	public void recordState(DialogueState state, String name) {
		if (frame != null) {
		stateMonitorTab.recordState(state, name);
		}
 	}


	public void addComment(String comment) {
		if (frame != null) {
		chatTab.addComment(comment);
		}
	}
	
	public ChatWindowTab getChatTab() {
		return chatTab;
	}
	
	public StateViewerTab getStateViewerTab() {
		return stateMonitorTab;
	}


	public JFrame getFrame() {
		return frame;
	}

}
