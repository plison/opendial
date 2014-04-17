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

package opendial.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import opendial.DialogueSystem;
import opendial.arch.Logger;
import opendial.modules.Module;
import opendial.modules.speech.SpeechRecogniser;
import opendial.state.DialogueState;

/**
 * Main GUI frame for the OpenDial toolkit, encompassing various tabs and
 * menus to control the application
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GUIFrame implements Module {

	// logger
	public static Logger log = new Logger("GUIFrame", Logger.Level.DEBUG);

	public static final String ICON_PATH = "resources/opendial-icon.png";

	JFrame frame;

	// tab for the state monitor
	StateViewerTab stateMonitorTab;
	
	// tab for the chat window
	ChatWindowTab chatTab;
	
	DialogueSystem system;		
	
	GUIMenuBar menu;

	
	/**
	 * Constructs (but does not yet display) a new GUI frame for OpenDial.
	 * 
	 * @param system the dialogue system for the GUI
	 */
	public GUIFrame(DialogueSystem system) {
		this.system = system;
	}
	
	
	/**
	 * Displays the GUI frame.
	 */
	@Override
	public void start() {
 

		if (system.getSettings().showGUI) {
		frame = new JFrame();
		try {
		frame.setIconImage(ImageIO.read(new File(ICON_PATH)));
		}
		catch (Exception e) {
			log.debug("could not employ icon: " + e);
		}
		JTabbedPane tabbedPane = new JTabbedPane();
		frame.getContentPane().add(tabbedPane);

		frame.setLocation(new Point(200, 200));

		frame.addWindowListener(new WindowAdapter() 
		{
			@Override
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
		trigger(system.getState(), new ArrayList<String>());
	}
	
	
	/**
	 * Pauses the GUI.
	 */
	@Override
	public void pause(boolean pause) {
		if (frame != null && frame.isVisible()) {
		chatTab.updateActivation();
		}
	}
	

	/**
	 * Returns the dialogue system connected to the GUI
	 * 
	 * @return the dialogue system
	 */
	public DialogueSystem getSystem() {
		return system;
	}

	/**
	 * Updates the current dialogue state displayed in the component.  The current
	 * dialogue state is name "current" in the selection list.
	 * 
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (frame != null && frame.isVisible()) {
		chatTab.trigger(state, updatedVars);
		stateMonitorTab.trigger(state, updatedVars);
		menu.update();
		if (system.getDomain() == null) {
			frame.setTitle("OpenDial toolkit");
		}
		else if (!frame.getTitle().contains(system.getDomain().getName())){
			frame.setTitle("OpenDial toolkit - domain: " + system.getDomain().getName());
		}
		chatTab.enableSpeechInput(system.getModule(SpeechRecogniser.class) != null);
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


 	/**
 	 * Adds a comment to the chat window
 	 * @param comment
 	 */
	public void addComment(String comment) {
		if (frame != null) {
		chatTab.addComment(comment);
		}
	}
	
	/**
	 * Returns the chat tab
	 * 
	 * @return the  chat tab
	 */
	public ChatWindowTab getChatTab() {
		return chatTab;
	}
	
	/**
	 * Returns the state viewer tab
	 * @return the state viewer
	 */
	public StateViewerTab getStateViewerTab() {
		return stateMonitorTab;
	}


	/**
	 * Returns the GUI frame itself.
	 * 
	 * @return the frame
	 */
	public JFrame getFrame() {
		return frame;
	}


	/**
	 * Returns true if the GUI is started and not paused, and false otherwise.
	 * 
	 * @return true if the GUI is running, false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return (frame != null && frame.isVisible());
	}

}
