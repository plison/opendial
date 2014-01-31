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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.java.balloontip.BalloonTip;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;


/**
 * GUI tab for the chat window.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class ChatWindowTab extends JComponent implements ActionListener {

	public static final String TAB_TITLE = " Chat Window ";
	public static final String TAB_TIP = "Chat window listing the user and system utterances";
	
	public static Logger log = new Logger("ChatWindowTab", Logger.Level.DEBUG); 

	// main chat window
	HTMLEditorKit kit;
    HTMLDocument doc;

	// input field
	JTextField inputField;
	JTextPane lines;
		
	DialogueSystem system;
	
	int nBestView = 20;
		
	/**
	 * Start up the window
	 * 
	 * @param system the dialogue system
	 */
	public ChatWindowTab (DialogueSystem system) 
	{
		this.system = system;
		
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

		inputField = new JTextField(60);		
		inputContainer.add(new JLabel("User input: "), BorderLayout.WEST);
		inputContainer.add(inputField, BorderLayout.CENTER);
		final JButton helpButton = new JButton( "" );
		helpButton.putClientProperty( "JButton.buttonType", "help" );
		
		final BalloonTip tip = new BalloonTip(helpButton, "<html><br>- To directly enter a user utterance, simply type it in the text field "
				+ "at<br>&nbsp;&nbsp;&nbsp;the bottom of the window, for instance: <br> "
				+ "<p style=\"font-size: 2px\">&nbsp;</p>&nbsp;&nbsp;&nbsp;<b>User input: </b><i>now move to the left</i><br><br>"
				+ "- To associate the utterance a recognition probability, simply enter the<br>"
				+ "&nbsp;&nbsp;&nbsp;probability value in parenthesis after the utterance:<br>"
				+ "<p style=\"font-size: 2px\">&nbsp;</p>&nbsp;&nbsp;&nbsp;<b>User input: </b><i>now move left (0.55)</i><br><br>"
				+ "&nbsp;&nbsp;&nbsp;Probability values must be comprised between 0 and 1. When the total<br>"
				+ "&nbsp;&nbsp;&nbsp;probability is lower than 1, the remaining probability mass is assigned <br>"
				+ "&nbsp;&nbsp;&nbsp;to a default \"none\" value (i.e. no recognition).<br><br>"
				+ "- Finally, to enter an N-best list of user utterances, separate each<br>"
				+ "&nbsp;&nbsp;&nbsp;alternative recognition hypothesis with a semicolon, as in:<br>"
				+ "<p style=\"font-size: 2px\">&nbsp;</p>&nbsp;&nbsp;&nbsp;<b>User input: </b><i>now move left (0.55) ; "
				+ "do not move left (0.15)<br><br></html>");
		tip.setVisible(false);
		 
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tip.setVisible(!tip.isVisible());
			}
		});
		helpButton.setFocusable(false);
		
		inputContainer.add(helpButton, BorderLayout.EAST);
		// Add the text field and the utterances
		add (inputContainer, BorderLayout.SOUTH);
		add (utterancesScrollPane, BorderLayout.CENTER);
		//	setPreferredSize(new Dimension(380,380));

		inputField.addActionListener(this);	
				
		kit = new HTMLEditorKit();
	    doc = new HTMLDocument();
	    lines.setEditorKit(kit);
	    lines.setDocument(doc);
	    lines.setFocusable(false);
	    updateActivation();
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
	
	
	public void setNBest(int nBestView) {
		this.nBestView = nBestView;
	}
	
	public int getNBest() {
		return nBestView;
	}



	private void addUtteranceToState()  {
		String rawText= inputField.getText();
		if (!rawText.equals("")) {
			String[] splitText = rawText.split(";");

			CategoricalTable table = new CategoricalTable();
			
			for (String split : Arrays.asList(splitText)) {				
				Map.Entry<String, Float> split2 = getProbabilityValueInParenthesis(split);
				table.addRow(new Assignment(system.getSettings().userInput, split2.getKey()), split2.getValue());
			}

			inputField.setText("");
			(new StateUpdater(system, table)).start();
			
		}
	}
	

	public void addComment(String comment) {
		try {
		    kit.insertHTML(doc, doc.getLength(),"[" +comment + "]\n", 0, 0, null);
			}
			catch (Exception e) {
				log.warning("text area exception: " + e);
			}
	}



	/**                                                                                                                                                                
	 * If the probability value of a given input is provided in parenthesis,                                                                                           
	 * try to extract it                                                                                                                                               
	 *                                                                                                                                                                 
	 * @param text the string where the probability value might be                                                                                                     
	 * @return the probability value if a valid one is entered, else 1.0f                                                                                              
	 */
	private Map.Entry<String, Float> getProbabilityValueInParenthesis (String text) {

		try {
			Pattern p = Pattern.compile(".*\\(([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)\\).*");
			Matcher m = p.matcher(text);
			if (m.find()) {
				String probValueStr = m.group(1);
				float probValue = Float.parseFloat(probValueStr);
				String remainingStr = text.replace("(" + probValueStr + ")", "").trim();
				return new AbstractMap.SimpleEntry<String,Float>(remainingStr, probValue);
			}
			return new AbstractMap.SimpleEntry<String,Float>(text.trim(), 1.0f);
		}
		catch (Exception e) {
			return new AbstractMap.SimpleEntry<String,Float>(text.trim(), 1.0f);
		}
	}
	
	

	private String getHtmlRendering(CategoricalTable table) {

		String htmlTable = "";
		for (String var : table.getHeadVariables()) {
			String baseVar = var.replace("'", "");
			htmlTable += "<p style=\"font-size:2px;\"><table><tr><td width=100><font size=4>";
			
			if (baseVar.equals(system.getSettings().userInput)) {
				htmlTable += "<b>[user]</b>";
			}
			else if (baseVar.equals(system.getSettings().systemOutput)) {
				htmlTable += "<b>[system]</b>";
			}
			else {
				htmlTable += "[" + baseVar + "]";
			}
			htmlTable += "</font></td>";
			CategoricalTable marginalTable = table.getMarginalTable(var);
			for (Assignment a : marginalTable.getRows()) {
				Value value = a.getValue(var);
				if (!(value instanceof NoneVal)) {
					htmlTable += "<td><font size=4>";
					String content = value.toString();
					if (marginalTable.getProb(a) < 0.98) {
						content += " (" + StringUtils.getShortForm(marginalTable.getProb(a)) + ")";
					}
					if (system.getSettings().varsToMonitor.contains(baseVar)) {
						content = "<i>" + content + "</i>";
					}
					htmlTable += content + "</font></td></tr><tr><td></td>";
				}
			}
			htmlTable = htmlTable.substring(0, htmlTable.length() - 13) + "</table></p>\n";		
		}
		return htmlTable;		
	}

	
	void updateActivation() {
		if (system.getDomain() == null) {
			inputField.setEnabled(false);
			lines.setEnabled(false);
			if (lines.getText().length() <= 100 && ! lines.getText().contains("No domain currently selected")) {
				addComment("No domain currently selected");
			}
		}
		else {
			if (inputField.isEnabled() == system.isPaused()) {
			inputField.setEnabled(!system.isPaused());
			lines.setEnabled(!system.isPaused());
			}
		}
	}


	public void trigger(DialogueState state, Collection<String> updatedVars) {
		updateActivation();
		if (updatedVars.contains(system.getSettings().userInput)
				&& state.hasChanceNode(system.getSettings().userInput)) {	
			try {
			DiscreteDistribution distrib = state.getChanceNode(system.getSettings().userInput).getDistrib().toDiscrete();
			if (distrib instanceof CategoricalTable) {
				showVariable((CategoricalTable)distrib);
			}
			else {
				showVariable(state.queryProb(system.getSettings().userInput).toDiscrete());
			}
			}
			catch (DialException e) {
				log.warning("cannot add utterance: " + e);
			}
		}
		if (updatedVars.contains(system.getSettings().systemOutput)
				&& state.hasChanceNode(system.getSettings().systemOutput)) {
			showVariable(state.queryProb(system.getSettings().systemOutput).toDiscrete());
		}
		for (String monitorVar : system.getSettings().varsToMonitor) {
			if (updatedVars.contains(monitorVar)) {
				showVariable(state.queryProb(monitorVar).toDiscrete());
			}
		}
	} 

	private void showVariable(CategoricalTable distrib) {
		distrib = (distrib.getBest().isDefault())? distrib.getNBest(nBestView+1) : distrib.getNBest(nBestView);
		String text = getHtmlRendering(distrib);
		try {
	    kit.insertHTML(doc, doc.getLength(),text, 0, 0, null);
		}
		catch (Exception e) {
			log.warning("text area exception: " + e);
		}
	}

	

	public String getChat() {
		return lines.getText();
	}


	final class StateUpdater extends Thread {
		
		DialogueSystem system;
		CategoricalTable table;
		
		public StateUpdater(DialogueSystem system, CategoricalTable table) {
			this.system = system;
			this.table = table;
		}
		
		@Override
		public void run() {
			try {
			system.addContent(table);
			}
			catch (DialException e) {
				log.warning("cannot update state with user utterance");
			}
		}
	}


}
