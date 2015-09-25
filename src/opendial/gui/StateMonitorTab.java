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

import java.util.logging.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import opendial.DialogueState;
import opendial.Settings;
import opendial.Settings.Recording;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.UtilityFunction;
import opendial.gui.utils.StateViewer;
import opendial.utils.StringUtils;

/**
 * GUI component used to view and control the Bayesian Network representing the
 * dialogue state.
 * 
 * <p>
 * The component includes functionalities for:
 * <ul>
 * <li>rendering a full graphical version of the network, including all types of
 * nodes and their respective dependencies;
 * <li>zooming and translating the network in various directions;
 * <li>providing details about the random variables and their distribution;
 * <li>performing runtime inference over selected sets of random variables;
 * <li>navigating through different recorded versions of the network.
 * </ul>
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
@SuppressWarnings("serial")
public class StateMonitorTab extends JComponent {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// title, position and tooltip for the tab
	public static final String TAB_TITLE = " State Monitor ";
	public static final String TAB_TIP =
			"Visual monitoring as the Bayesian Network defining the dialogue state";

	// directions for zooming and translating the graph
	public static enum ZoomDirection {
		IN, OUT
	}

	public static enum TranslationDirection {
		NORTH, SOUTH, EAST, WEST
	}

	// the frame including the tab
	GUIFrame mainFrame;

	// list model for the recorded networks
	DefaultListModel<String> listModel;
	JList<String> listBox;

	// states available for visualisation
	Map<String, DialogueState> states;

	// the Bayesian Network viewer
	StateViewer visualisation;

	// the logging area at the bottom
	JEditorPane logArea;

	public static String CURRENT = "<html><b>Current state</b></html>";

	boolean showParameters = true;

	/**
	 * Creates a new GUI component for displaying and controlling the dialogue
	 * state's Bayesian Network.
	 * 
	 * @param mainFrame the reference to the main GUI frame.
	 * 
	 */
	public StateMonitorTab(GUIFrame mainFrame) {
		setLayout(new BorderLayout());

		this.mainFrame = mainFrame;

		states = new HashMap<String, DialogueState>();

		// create the left side of the window
		JPanel leftPanel = createLeftSide();

		// create the main visualisation window
		visualisation = new StateViewer(this);

		// create the logging area
		logArea = createLogArea();
		JScrollPane logScroll = new JScrollPane(logArea);
		logScroll.setBorder(BorderFactory.createEmptyBorder());

		// arrange the global layout
		JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
				visualisation.wrapWithScrollPane());
		topPanel.setDividerLocation(200);
		JSplitPane fullPanel =
				new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, logScroll);
		fullPanel.setDividerLocation(600);
		add(fullPanel);

		// configure the keyboard inputs for navigation
		configureKeyInputs();

		recordState(new DialogueState(), CURRENT);
		listModel.add(1, "separator-current");
	}

	/**
	 * Sets whether to show the parameter variables in the state viewer or not.
	 * 
	 * @param showParameters whether to show or hide the parameters
	 */
	public void showParameters(boolean showParameters) {
		this.showParameters = showParameters;
		if (states.containsKey(CURRENT)) {
			refresh(mainFrame.getSystem().getState(),
					mainFrame.getSystem().getState().getParameterIds());
		}
	}

	/**
	 * Returns true if the parameter variables are currently displayed in the state
	 * viewer. Returns false otherwise.
	 * 
	 * @return true if parameters are shown, false otherwise.
	 */
	public boolean showParameters() {
		return showParameters;
	}

	/**
	 * Resets the dialogue state shown in the monitor tab.
	 * 
	 * @param state the dialogue state to reset
	 */
	public void reset(DialogueState state) {
		recordState(state, CURRENT);
		listBox.setSelectedIndex(0);
		while (listModel.size() > 2) {
			String name = listModel.remove(2);
			states.remove(name);
		}
		visualisation.showBayesianNetwork(state);
	}

	/**
	 * Updates the current dialogue state displayed in the component. The current
	 * dialogue state is named "Current state" in the selection list.
	 * 
	 * @param state the updated Bayesian Network
	 * @param updatedVars the updated variables
	 */
	public void refresh(DialogueState state, Collection<String> updatedVars) {

		recordState(state, CURRENT);
		listBox.setSelectedIndex(0);
		Settings settings = mainFrame.getSystem().getSettings();
		if (updatedVars.contains(settings.userInput)) {
			if (settings.recording == Recording.ALL) {
				listModel.add(2, "separator-utterances");
			}
			else {
				while (listModel.size() > 2) {
					String name = listModel.remove(2);
					states.remove(name);
				}
			}
		}

		Set<String> chanceVars = state.getNewVariables();
		Set<String> actionVars = state.getNewActionVariables();

		String title = "";
		if (!chanceVars.isEmpty()) {
			title = "Updating " + StringUtils.join(chanceVars, ",");
		}
		if (!actionVars.isEmpty()) {
			title += (!title.isEmpty()) ? ", " : "";
			title += "Selecting " + StringUtils.join(actionVars, ",");
		}
		if (settings.recording != Recording.NONE && !title.isEmpty()) {
			title += "[" + System.currentTimeMillis() + "]";
			try {
				recordState(state.copy(), title);
			}
			catch (RuntimeException e) {
				log.warning("cannot copy state : " + e);
			}
		}

		visualisation.showBayesianNetwork(state);
	}

	/**
	 * Records a dialogue state in the component and makes it available for display
	 * in the network selection list on the left side. The network is associated with
	 * a specific name. If the name already exists, the previous network is erased.
	 * 
	 * 
	 * @param state the dialogue state to record
	 * @param name the name for the recorded network
	 */
	public void recordState(DialogueState state, String name) {
		states.put(name, state);
		if (!listModel.contains(name)) {
			int position =
					name.contains(CURRENT) ? 0 : Math.min(2, listModel.size());
			listModel.add(position, name);
		}

	}

	/**
	 * Writes the given distribution in the logging area at the bottom of the window
	 * 
	 * @param distrib the distribution to write
	 */
	public void writeToLogArea(MultivariateDistribution distrib) {
		String distribStr = distrib.toString().replace("\n", "\n<br>");
		distribStr = StringUtils.getHtmlRendering(distribStr);
		logArea.setText("<html><font size=\"4\" face=\"helvetica\">" + distribStr
				+ "</font></html>");
	}

	/**
	 * Writes the given distribution in the logging area at the bottom of the window
	 * 
	 * @param distrib the distribution to write
	 */
	public void writeToLogArea(UtilityFunction distrib) {
		String distribStr = distrib.toString().replace("\n", "\n<br>");
		distribStr = StringUtils.getHtmlRendering(distribStr);
		logArea.setText("<html><font  size=\"4\" face=\"helvetica\">" + distribStr
				+ "</font></html>");
	}

	/**
	 * Returns the reference to the main GUI.
	 * 
	 * @return the GUI frame.
	 */
	public GUIFrame getMainFrame() {
		return mainFrame;
	}

	// ===================================
	// PRIVATE METHODS
	// ===================================

	/**
	 * Creates the panel on the left side of the window
	 * 
	 * @return the panel
	 */
	private JPanel createLeftSide() {
		JPanel leftPanel = new JPanel();
		listModel = new CustomListModel();
		leftPanel.setLayout(new BorderLayout());
		listBox = new JList<String>(listModel);
		listBox.setCellRenderer(new JlistRenderer());
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
		plus.addMouseListener(
				new CustomMouseListener(new ZoomAction(ZoomDirection.IN)));
		zoomPanel.add(plus, BorderLayout.NORTH);
		JButton minus = new JButton("-");
		minus.addMouseListener(
				new CustomMouseListener(new ZoomAction(ZoomDirection.OUT)));
		zoomPanel.add(minus, BorderLayout.SOUTH);

		controlPanel.add(zoomPanel, BorderLayout.WEST);
		controlPanel.add(new JSeparator(SwingConstants.VERTICAL),
				BorderLayout.CENTER);

		// translation buttons
		Container translationPanel = new Container();
		translationPanel.setLayout(new BorderLayout());
		JButton up = new BasicArrowButton(SwingConstants.NORTH);
		up.addMouseListener(new CustomMouseListener(
				new TranslationAction(TranslationDirection.NORTH)));
		translationPanel.add(up, BorderLayout.NORTH);
		JButton west = new BasicArrowButton(SwingConstants.WEST);
		west.addMouseListener(new CustomMouseListener(
				new TranslationAction(TranslationDirection.WEST)));
		translationPanel.add(west, BorderLayout.WEST);
		translationPanel.add(new JLabel("        "), BorderLayout.CENTER);
		JButton east = new BasicArrowButton(SwingConstants.EAST);
		east.addMouseListener(new CustomMouseListener(
				new TranslationAction(TranslationDirection.EAST)));
		translationPanel.add(east, BorderLayout.EAST);
		JButton south = new BasicArrowButton(SwingConstants.SOUTH);
		south.addMouseListener(new CustomMouseListener(
				new TranslationAction(TranslationDirection.SOUTH)));
		translationPanel.add(south, BorderLayout.SOUTH);

		controlPanel.add(translationPanel, BorderLayout.EAST);
		controlPanel.setBorder(
				new CompoundBorder(BorderFactory.createTitledBorder("Controls"),
						BorderFactory.createEmptyBorder(5, 5, 10, 20)));

		return controlPanel;
	}

	/**
	 * Configure the keyboard inputs for zooming and translating the network in
	 * various directions.
	 * 
	 * (minor) todo: check that the keyboard codes are universal
	 */
	private void configureKeyInputs() {
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(45, 0, false), ZoomDirection.IN);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(47, 0, false), ZoomDirection.OUT);

		// NB: the translation actions are only made available when the graph is
		// in focus,
		// to avoid conflicts with the navigation of the selection list
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(38, 0, false), TranslationDirection.NORTH);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(40, 0, false), TranslationDirection.SOUTH);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(37, 0, false), TranslationDirection.WEST);
		visualisation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(39, 0, false), TranslationDirection.EAST);

		getActionMap().put(ZoomDirection.OUT, new ZoomAction(ZoomDirection.OUT));
		getActionMap().put(ZoomDirection.IN, new ZoomAction(ZoomDirection.IN));
		visualisation.getActionMap().put(TranslationDirection.NORTH,
				new TranslationAction(TranslationDirection.NORTH));
		visualisation.getActionMap().put(TranslationDirection.SOUTH,
				new TranslationAction(TranslationDirection.SOUTH));
		visualisation.getActionMap().put(TranslationDirection.WEST,
				new TranslationAction(TranslationDirection.WEST));
		visualisation.getActionMap().put(TranslationDirection.EAST,
				new TranslationAction(TranslationDirection.EAST));
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
		Insets inset = new Insets(10, 5, 10, 10);
		textArea.setMargin(inset);
		textArea.setSize(new Dimension(900, 100));
		return textArea;
	}

	/**
	 * A listener for the list containing the Bayesian Networks. Once a network is
	 * selected, its graph is automatically displayed in the main window.
	 *
	 */
	final class CustomListSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			@SuppressWarnings("unchecked")
			JList<String> jl = (JList<String>) e.getSource();
			if (jl.getMinSelectionIndex() >= 0 && !e.getValueIsAdjusting()) {
				String selection = listModel.getElementAt(jl.getMinSelectionIndex());
				if (!selection.contains("separator")) {
					visualisation.showBayesianNetwork(states.get(selection));
				}
			}
		}
	}

	/**
	 * Representation of a zooming action, which can be either IN or OUT:
	 * 
	 */
	final class ZoomAction extends AbstractAction {

		ZoomDirection direction;

		public ZoomAction(ZoomDirection direction) {
			this.direction = direction;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			switch (direction) {
			case IN:
				visualisation.zoomIn();
				break;
			case OUT:
				visualisation.zoomOut();
				break;
			}
		}
	}

	/**
	 * Representation of a translation action, which can navigate in the four
	 * cardinal directions: NORTH, SOUTH, WEST and EAST.
	 *
	 */
	final class TranslationAction extends AbstractAction {

		TranslationDirection direction;

		public TranslationAction(TranslationDirection direction) {
			this.direction = direction;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			switch (direction) {
			case NORTH:
				visualisation.translate(0, 10);
				break;
			case SOUTH:
				visualisation.translate(0, -10);
				break;
			case WEST:
				visualisation.translate(-10, 0);
				break;
			case EAST:
				visualisation.translate(10, 0);
				break;
			}
		}
	}

	/**
	 * Custom mouse listener for the control buttons. The objective of this button is
	 * to keep pursuing the given action (either zooming or translating) until the
	 * button is released. This is realised via a thread which is interrupted upon
	 * the button release.
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
			movement = new Thread() {
				@Override
				public void run() {
					boolean continueProcess = true;
					while (continueProcess) {
						try {
							action.actionPerformed(new ActionEvent(this, 0, ""));
							Thread.sleep(100);
						}
						catch (InterruptedException e) {
							continueProcess = false;
						}
					}
				}
			};
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

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

	/**
	 * Custom model for the list of dialogue states (employed to avoid null pointer
	 * exceptions).
	 */
	final class CustomListModel extends DefaultListModel<String> {

		@Override
		public String getElementAt(int index) {
			if (index < super.size()) {
				try {
					return super.getElementAt(index);
				}
				catch (ArrayIndexOutOfBoundsException e) {
					return "";
				}
			}
			else {
				return "";
			}
		}
	}

	/**
	 * Renderer for the list of dialogue states
	 */
	final class JlistRenderer extends JLabel implements ListCellRenderer<String> {
		JSeparator separator;

		public JlistRenderer() {
			setOpaque(true);
			setBorder(new EmptyBorder(1, 1, 1, 1));
			separator = new JSeparator(SwingConstants.HORIZONTAL);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list,
				String value, int index, boolean isSelected, boolean cellHasFocus) {
			String str = (value == null) ? "" : value.toString();
			if (str.contains("separator")) {
				return separator;
			}
			if (str.contains("[")) {
				str = str.substring(0, str.indexOf("["));
			}
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText(str);
			return this;
		}
	}

}
