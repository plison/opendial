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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import opendial.arch.Logger;
import opendial.gui.stateviewer.StateViewer;
import opendial.state.DialogueState;


/**
 * GUI component used to view and control the Bayesian Network representing the dialogue
 * state.  
 * 
 * <p>The component includes functionalities for: <ul>
 * <li> rendering a full graphical version of the network, including all types of nodes 
 *      and their respective dependencies;
 * <li> zooming and translating the network in various directions;
 * <li> providing details about the random variables and their distribution;
 * <li> performing runtime inference over selected sets of random variables;
 * <li> navigating through different recorded versions of the network.
 * </ul>
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class StateViewerTab extends JComponent {

	// logger
	public static Logger log = new Logger("StateViewerTab", Logger.Level.DEBUG);

	// title, position and tooltip for the tab
	public static final String TAB_TITLE = " Dialogue State Monitor ";
	public static final String TAB_TIP = "Visual monitoring as the Bayesian Network defining the dialogue state";

	// directions for zooming and translating the graph
	public static enum ZoomDirection {IN, OUT}
	public static enum TranslationDirection {NORTH, SOUTH, EAST, WEST}

	// the frame including the tab
	JFrame mainFrame;
	
	// list model for the recorded networks
	DefaultListModel listModel;
	JList listBox;

	// states available for visualisation
	Map<String,DialogueState> states;

	// the Bayesian Network viewer
	StateViewer visualisation;

	// the logging area at the bottom
	JEditorPane logArea;
	
	
	boolean showParameters = true;
	

	/**
	 * Creates a new GUI component for displaying and controlling the dialogue state's
	 * Bayesian Network.
	 * 
	 */
	public StateViewerTab(GUIFrame mainFrame) {
		setLayout(new BorderLayout());
		
		this.mainFrame = mainFrame.getFrame();

		states = new HashMap<String,DialogueState>();
		
		// create the left side of the window
		JPanel leftPanel = createLeftSide();
		
		// create the main visualisation window
		visualisation = new StateViewer(this);
		
		// create the logging area
		logArea = createLogArea();
		JScrollPane logScroll = new JScrollPane(logArea);
		logScroll.setBorder(BorderFactory.createEmptyBorder());		


		// arrange the global layout
		JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,visualisation.wrapWithScrollPane());
		topPanel.setDividerLocation(200);
		JSplitPane fullPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, logScroll);
		fullPanel.setDividerLocation(600);
		add(fullPanel);
	
		// configure the keyboard inputs for navigation
		configureKeyInputs();	
	}


	public void showParameters(boolean showParameters) {
		this.showParameters = showParameters;
		if (states.containsKey("current")) {
			update(states.get("current"));
		}
	}
	
	
	public boolean showParameters() {
		return showParameters;
	}
	
	/**
	 * Updates the current dialogue state displayed in the component.  The current
	 * dialogue state is name "current" in the selection list.
	 * 
	 * @param state the updated Bayesian Network
	 */
	public void update(DialogueState state) {
		recordState(state, "current");
		listBox.setSelectedIndex(0);
		visualisation.showBayesianNetwork(state);
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
		states.put(name, state);
		if (!listModel.contains(name)) {
			int position = name.equals("current") ? 0 : Math.min(1, listModel.size()) ; 
			listModel.add(position,name);
		}
		listBox.validate();
		listBox.repaint();
	}
	
	/**
	 * Writes the given text in the logging area at the bottom of the window
	 * 
	 * @param text the text to write (can be HTML code)
	 */
	public void writeToLogArea(String text) {
		logArea.setText("<html><font face=\"helvetica\">"+ text + "</font></html>");
	}
	
	public JFrame getMainFrame() {
		return mainFrame;
	}

	
	// ===================================
	//  PRIVATE METHODS
	// ===================================
	
	

	/**
	 * Creates the panel on the left side of the window
	 * 
	 * TODO: functionality to delete sets of recorded states from the panel
	 * @return the panel
	 */
	private JPanel createLeftSide() {
		JPanel leftPanel = new JPanel();
		listModel = new DefaultListModel();
		leftPanel.setLayout(new BorderLayout());
		listBox = new JList(listModel);
		listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listBox.addListSelectionListener(new CustomListSelectionListener());
		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());		
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Dialogue states:"));

		JPanel controlPanel = createControlPanel();
		leftPanel.add(controlPanel, BorderLayout.SOUTH);
		return leftPanel;
	}

	/**
	 * Creates the small control panel below the selection list
	 * 
	 * @return the control panel
	 */
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());

		// zooming buttons
		Container zoomPanel = new Container();
		zoomPanel.setLayout(new BorderLayout());
		JButton plus = new JButton("+");
		plus.addMouseListener(new CustomMouseListener(new ZoomAction(ZoomDirection.IN))) ;
		zoomPanel.add(plus, BorderLayout.NORTH);
		JButton minus = new JButton("-");
		minus.addMouseListener(new CustomMouseListener(new ZoomAction(ZoomDirection.OUT))) ;
		zoomPanel.add(minus, BorderLayout.SOUTH);

		controlPanel.add(zoomPanel, BorderLayout.WEST);
		controlPanel.add(new JSeparator(JSeparator.VERTICAL), BorderLayout.CENTER);

		// translation buttons
		Container translationPanel = new Container();
		translationPanel.setLayout(new BorderLayout());
		JButton up = new BasicArrowButton(BasicArrowButton.NORTH);
		up.addMouseListener(new CustomMouseListener(new TranslationAction(TranslationDirection.NORTH))) ;
		translationPanel.add(up, BorderLayout.NORTH);
		JButton west = new BasicArrowButton(BasicArrowButton.WEST);
		west.addMouseListener(new CustomMouseListener(new TranslationAction(TranslationDirection.WEST))) ;
		translationPanel.add(west, BorderLayout.WEST);
		translationPanel.add(new JLabel("        "), BorderLayout.CENTER);
		JButton east = new BasicArrowButton(BasicArrowButton.EAST);
		east.addMouseListener(new CustomMouseListener(new TranslationAction(TranslationDirection.EAST))) ;
		translationPanel.add(east, BorderLayout.EAST);
		JButton south = new BasicArrowButton(BasicArrowButton.SOUTH);
		south.addMouseListener(new CustomMouseListener(new TranslationAction(TranslationDirection.SOUTH))) ;
		translationPanel.add(south, BorderLayout.SOUTH);

		controlPanel.add(translationPanel, BorderLayout.EAST);
		controlPanel.setBorder(new CompoundBorder(
				BorderFactory.createTitledBorder("Controls"), 
				BorderFactory.createEmptyBorder(5,5,10,20)));
		
		return controlPanel;
	}
	
	
	/**
	 * Configure the keyboard inputs for zooming and translating the network
	 * in various directions.
	 * 
	 * (minor) todo: check that the keyboard codes are universal
	 */
	private void configureKeyInputs() {
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(45,0,false), ZoomDirection.IN);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(47,0,false), ZoomDirection.OUT);

		// NB: the translation actions are only made available when the graph is in focus, 
		// to avoid conflicts with the navigation of the selection list
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(38,0,false), TranslationDirection.NORTH);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(40,0,false), TranslationDirection.SOUTH);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(37,0,false), TranslationDirection.WEST);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
			put(KeyStroke.getKeyStroke(39,0,false), TranslationDirection.EAST);

		getActionMap().put(ZoomDirection.OUT, new ZoomAction(ZoomDirection.OUT));		
		getActionMap().put(ZoomDirection.IN, new ZoomAction(ZoomDirection.IN));
		visualisation.getActionMap().
		put(TranslationDirection.NORTH, new TranslationAction(TranslationDirection.NORTH));	
		visualisation.getActionMap().
		put(TranslationDirection.SOUTH, new TranslationAction(TranslationDirection.SOUTH));	
		visualisation.getActionMap().
		put(TranslationDirection.WEST, new TranslationAction(TranslationDirection.WEST));	
		visualisation.getActionMap().
		put(TranslationDirection.EAST, new TranslationAction(TranslationDirection.EAST));	
	}

	/**
	 * Creates the logging area at the bottom of the windo
	 * 
	 * @return the logging area
	 */
	private JEditorPane createLogArea() {		
		JEditorPane textArea = new JEditorPane();
		textArea.setContentType("text/html");
		textArea.setEditable(false);
		Insets inset = new Insets(10,5,10,10);
		textArea.setMargin(inset);
		textArea.setSize(new Dimension(900,100));
		return textArea;
	}

	
	
	/**
	 * A listener for the list containing the Bayesian Networks.  Once
	 * a network is selected, its graph is automatically displayed in the main 
	 * window.
	 *
	 */
	final class CustomListSelectionListener implements ListSelectionListener {


		@Override
		public void valueChanged(ListSelectionEvent e) {
			JList jl = (JList)e.getSource();
			String selection = (String) listModel.getElementAt(jl.getMinSelectionIndex());
			visualisation.showBayesianNetwork(states.get(selection));
		}
	}
	
	
	/**
	 * Representation of a zooming action, which can be either IN or OUT:
	 * 
	 */
	final class ZoomAction extends AbstractAction {

		ZoomDirection direction ;
		
		
		public ZoomAction(ZoomDirection direction) {
			this.direction = direction;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			switch (direction) {
			case IN : visualisation.zoomIn(); break;
			case OUT: visualisation.zoomOut(); break;
			}
		}	
	}

	
	/**
	 * Representation of a translation action, which can navigate in the four
	 * cardinal directions: NORTH, SOUTH, WEST and EAST.
	 *
	 */
	final class TranslationAction extends AbstractAction {

		TranslationDirection direction ;
		
		public TranslationAction(TranslationDirection direction) {
			this.direction = direction;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			switch (direction) {
			case NORTH : visualisation.translate(0,10);  break; 
			case SOUTH:  visualisation.translate(0,-10); break; 
			case WEST: visualisation.translate(-10,0); break; 
			case EAST: visualisation.translate(10,0);break; 
			}
		}	
	}
	

	/**
	 * Custom mouse listener for the control buttons.  The objective of this button
	 * is to keep pursuing the given action (either zooming or translating) until the
	 * button is released.  This is realised via a thread which is interrupted upon the
	 * button release.
	 *
	 */
	final class CustomMouseListener implements MouseListener {

		AbstractAction action;
				Thread movement;

		public CustomMouseListener(AbstractAction action) {
			this.action = action;
			resetThread();
			
		}
		
		private void resetThread() {
			movement = new Thread() { public void run() { 
				boolean continueProcess = true;
				while (continueProcess) {
					try {
						action.actionPerformed(new ActionEvent(this,0,""));
						Thread.sleep(100);
					} 
					catch (InterruptedException e) { continueProcess=false; } 
				}
			}
			};
		}

		public void mouseClicked(MouseEvent e) {	}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {	}

		@Override
		public void mousePressed(MouseEvent e) {
			movement.start();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			movement.interrupt();
			resetThread();
		}
	}
	

}
