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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import opendial.arch.Logger;
import opendial.arch.StateListener;
import opendial.state.DialogueState;

/**
 * Main GUI frame for the openDial toolkit, encompassing various tabs and
 * menus to control the application
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
@SuppressWarnings("serial")
public class GUIFrame extends JFrame implements StateListener {

	// logger
	public static Logger log = new Logger("GUIFrame", Logger.Level.NORMAL);

	// frame instance
	private static GUIFrame guiFrameInstance;

	// tab for the state monitor
	StateMonitorTab stateMonitorTab;
	
	// tab for the chat window
	ChatWindowTab chatTab;
	
	DialogueState state;
	

	/**
	 * Constructs the GUI frame, with its title, menus, tabs etc.
	 * 
	 */
	public GUIFrame(DialogueState state) {
		
		this.state = state;
		
		Container contentPane = getContentPane();

		// TODO: add " - domain name " when a domain is loaded
		setTitle("OpenDial toolkit");
		
		JTabbedPane tabbedPane = new JTabbedPane();
		contentPane.add(tabbedPane);

		setLocation(new Point(200, 200));

		addWindowListener(new WindowAdapter() 
		{
			public void windowClosing(WindowEvent e) 
			{ System.exit(0); }
		}
		); 

		setJMenuBar(new ToolkitMenu(this));
		
		chatTab = new ChatWindowTab(this);
		tabbedPane.addTab(ChatWindowTab.TAB_TITLE, null, chatTab, ChatWindowTab.TAB_TIP);

		stateMonitorTab = new StateMonitorTab(this);
		tabbedPane.addTab(StateMonitorTab.TAB_TITLE, null, stateMonitorTab, StateMonitorTab.TAB_TIP);
		
		setPreferredSize(new Dimension(1000,800));
		pack();
		setVisible(true);
	}



	/**
	 * Updates the current dialogue state displayed in the component.  The current
	 * dialogue state is name "current" in the selection list.
	 * 
	 */
	@Override
	public synchronized void update() {
		chatTab.update();
		stateMonitorTab.update();
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
	public synchronized void recordState(DialogueState state, String name) {
		stateMonitorTab.recordState(state, name);
	}

	
	public DialogueState getConnectedState() {
		return state;
	}


}
