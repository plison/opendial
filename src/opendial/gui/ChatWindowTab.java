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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.state.DialogueState;


/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ChatWindowTab extends JComponent implements ActionListener, StateListener {

	public static final String TAB_TITLE = " Chat Window ";
	public static final String TAB_TIP = "Chat window listing the user and system utterances";

	public static Logger log = new Logger("ChatWindow", Logger.Level.DEBUG); 

	// main chat window
	JTextPane lines;
	HTMLEditorKit kit;
    HTMLDocument doc;

	// input field
	JTextField inputField;
	JComboBox agentBox;		

	JList listBox;

	GUIFrame mainFrame;
	
	Map<String,String> variablesToMonitor;

	/**
	 * Start up the window
	 * 
	 * @param tester reference to the live-testing environment
	 */
	public ChatWindowTab (GUIFrame mainFrame) 
	{
		setLayout(new BorderLayout());
		// Create the area where the utterances appear
		lines = new JTextPane();
		lines.setSize(30,50);
		lines.setContentType("text/html");
		lines.setMargin(new Insets(5,5,5,5));
		lines.setEditable(false);
		JScrollPane utterancesScrollPane = new JScrollPane(lines);


		Container inputContainer = new Container();
		inputContainer.setLayout(new BorderLayout());

		String[] agents = {"user", "system"};
		agentBox = new JComboBox(agents);
		agentBox.setSelectedIndex(0);
		inputField = new JTextField(60);
		Container agentContainer = new Container();
		agentContainer.setLayout(new BorderLayout());
		agentContainer.add(new JLabel(" as "), BorderLayout.WEST);
		agentContainer.add(agentBox, BorderLayout.EAST);
		inputContainer.add(agentContainer, BorderLayout.EAST);
		inputContainer.add(inputField, BorderLayout.CENTER);
		// Add the text field and the utterances
		add (inputContainer, BorderLayout.SOUTH);
		add (utterancesScrollPane, BorderLayout.CENTER);
		//	setPreferredSize(new Dimension(380,380));

		inputField.addActionListener(this);	
		
		this.mainFrame = mainFrame;

		variablesToMonitor = new HashMap<String,String>();
		variablesToMonitor.put(Settings.userUtteranceVar, "user");
		variablesToMonitor.put(Settings.systemUtteranceVar, "system");
		for (String var : Settings.varsToMonitor) {
			variablesToMonitor.put(var, var);
		}
		
		kit = new HTMLEditorKit();
	    doc = new HTMLDocument();
	    lines.setEditorKit(kit);
	    lines.setDocument(doc);
	} 


	/**
	 * Returns the text in the input field
	 * @return
	 */
	public String getInputText() {
		return inputField.getText();
	}


	public void addVariableToMonitor(String variable, String guiLabel) {
		variablesToMonitor.put(variable, guiLabel);
	}


	/**
	 * Sets the text in the input field to a particular value
	 * @param text
	 */
	public synchronized void setInputText(String text) {
		inputField.setText(text);
	}


	public String getCurrentAgent() {
		return agentBox.getSelectedItem().toString();
	}




	private synchronized void insertLine(SimpleTable table) throws DialException {

		if (table.getHeadVariables().size() != 1) {
			throw new DialException("table fed into GUI is incorrectly formatted, " +
					"variables are: " + table.getHeadVariables());
		}

		String variable = table.getHeadVariables().iterator().next();
		if (variablesToMonitor.containsKey(variable)) {
			String formattedTable = "";
			if (!table.isEmpty()) {
				int incr = 0;
				for (Assignment a : table.getRows()) {
					Value value = a.getValue(variable);
					if (!(value instanceof NoneVal)) {
						if (incr > 0) {
							formattedTable += "</font></td></tr><tr><td></td><td><font size=4>";
						}
						formattedTable += value;
						double prob = table.getProb(a);
						if (prob < 0.98) {
							prob = Math.round(table.getProb(a)*1000.0)/1000.0;
							formattedTable += " (" + prob + ")";
						}
						incr++;
					}
				}
				formattedTable += "\n";
			}
			
			String guiLabel = "<b>[" + variablesToMonitor.get(variable) + "]</b>";
			String newText =  "<p style=\"font-size: 2px;\"><table><tr><td width=100><font size=4>" + guiLabel +
					"</font></td><td><font size=4>" + formattedTable + "</font></td></tr></table></p>";
			
			if (!variable.equals(Settings.userUtteranceVar) && 
					!variable.equals(Settings.systemUtteranceVar)) {
				newText = newText.replace("<font", "<i><font").replace("<b>", "")
						.replace("</b>", "").replace("</font>", "</font></i>");
			}
			
			try {
		    kit.insertHTML(doc, doc.getLength(),newText, 0, 0, null);
			}
			catch (Exception e) {
				log.warning("text area exception: " + e);
			}
		    
		}
		lines.repaint();		
	}
	
	
	
	
	/**
	 * 
	 * @param string
	 */
	public void addActionSelection(List<String> actions, ActionListener listener) {

		DefaultListModel model = new DefaultListModel();
		for (String s : actions) {
			model.addElement(s);
		}
		listBox = new JList(model);
		listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());		
		scrollPane.setPreferredSize(new Dimension(200, 600));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Actions to select:"));

		Container container = new Container();
		container.setLayout(new BorderLayout());
		container.add(scrollPane);
		JButton button = new JButton("Select");
		button.addActionListener(listener);

		InputMap inputMap = button.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(enter, "ENTER");
		button.getActionMap().put("ENTER", new ClickAction(button));


		container.add(button, BorderLayout.SOUTH);
		add(container, BorderLayout.EAST);
		repaint();
	}


	/**
	 * 
	 * @return
	 */
	public String getSelectedAction() {
		return listBox.getModel().getElementAt(listBox.getMinSelectionIndex()).toString();
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().getClass().equals(JTextField.class)) {
			addUtteranceToState();
		}
		/** 	else if (e.getSource().getClass().equals(JButton.class)) {
			addSelectedAction();
		} */
	}



	private void addUtteranceToState()  {
		String rawText= getInputText();
		if (!rawText.equals("")) {
			String[] splitText = rawText.split(";");
			String var  ="";
			if (getCurrentAgent().equals("user")) {
				var = Settings.userUtteranceVar;
			}
			else if (getCurrentAgent().equals("system")) {
				var = Settings.systemUtteranceVar;
			}

			SimpleTable table = new SimpleTable();
			for (String split : Arrays.asList(splitText)) {
				double probValue = 1.0;
				if (split.contains("(") && split.contains(")")) {
					probValue = getProbabilityValueInParenthesis(split);
					split = split.substring(0, split.indexOf('('));
				}
				table.addRow(new Assignment(var, split.trim()), probValue);
			}

			try {
				insertLine(table);
				setInputText("");
				(new StateUpdater(mainFrame.getConnectedState(), table)).start();
			}
			catch (DialException e) {
				log.warning("cannot add utterance " + table + " to dialogue state");
			}
		}
	}


	/**                                                                                                                                                                
	 * If the probability value of a given input is provided in parenthesis,                                                                                           
	 * try to extract it                                                                                                                                               
	 *                                                                                                                                                                 
	 * @param text the string where the probability value might be                                                                                                     
	 * @return the probability value if a valid one is entered, else 1.0f                                                                                              
	 */
	private float getProbabilityValueInParenthesis (String text) {

		try {
			Pattern p = Pattern.compile(".*\\((\\d\\.\\d+)\\).*");
			Matcher m = p.matcher(text);
			if (m.find()) {
				return Float.parseFloat(m.group(1));
			}
			return 1.0f;
		}
		catch (Exception e) {
			return 1.0f;
		}
	}



	@Override
	public void update() {
		DialogueState state = mainFrame.getConnectedState();
		for (String updatedVar : state.getUpdatedVariables()) {
			String baseVar = updatedVar.replace("'", "");
			if (variablesToMonitor.containsKey(baseVar) && 
					!baseVar.equals(Settings.userUtteranceVar)) {
				try {
					ProbDistribution distrib = state.getContent(baseVar);
					if (distrib.toDiscrete() instanceof SimpleTable) {
						insertLine((SimpleTable)distrib.toDiscrete());
					}
				}
				catch (DialException e) {
					log.warning("cannot update " + updatedVar + " in the chat window: " + e);
				}
			}
		}
	}


	/** 
	private void addSelectedAction() {
		String action = getSelectedAction();

		String instantiatedAction = action.toString();
		List<String> slots = SurfaceTemplate.getSlots(action);
		for (String slot : slots) {
			if (state.hasVarNode(slot)) {
				instantiatedAction = instantiatedAction.replace("{"+slot+"}", 
						state.getVarNode(slot).getValueWithHighestProb(false).toString());
			}
		}		
		String varLabel = (actions.contains(action)) ? getParameter("systemActionVariable") : getParameter("systemCognitionVariable") ;
		Assignment selectedAction =  new Assignment(varLabel, action);
		if (varLabel.equals(getParameter("systemActionVariable")) && 
				lastSelectedAction.keySet().iterator().next().equals(getParameter("systemActionVariable")) && !action.equals("AskConfirmation")) {
			String implicitVarLabel = getParameter("systemCognitionVariable");
			Assignment implicitAction = new Assignment(implicitVarLabel, "None");
			recorder.recordTrainingData(new TrainingData(state, implicitAction));
		}

		recorder.recordTrainingData(new TrainingData(state,selectedAction));


		VarNode<String> node = new VarNode<String>(varLabel, String.class);
		node.addProb(instantiatedAction, 1.0f);



		lastSelectedAction = selectedAction;
		state.updateNode(node, Change.NEW, null);
	} */

	final class ClickAction extends AbstractAction {
		private JButton button;

		public ClickAction(JButton button) {
			this.button = button;
		}

		public void actionPerformed(ActionEvent e) {
			button.doClick();
		}
	}


	final class StateUpdater extends Thread {
		
		DialogueState state;
		SimpleTable table;
		
		public StateUpdater(DialogueState state, SimpleTable table) {
			this.state = state;
			this.table = table;
		}
		
		public void run() {
			try {
			state.addContent(table, "GUI");
			}
			catch (DialException e) {
				log.warning("cannot update state with user utterance");
			}
		}
	}


}
