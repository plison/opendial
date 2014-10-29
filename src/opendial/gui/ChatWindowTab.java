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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.io.StringReader;
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
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.java.balloontip.BalloonTip;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.gui.audio.SpeechInputPanel;
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
public class ChatWindowTab extends JComponent {

	public static final String TAB_TITLE = " Chat Window ";
	public static final String TAB_TIP = "Chat window listing the user and system utterances";
	public static final String TIP_TEXT = "<html><br>- To directly enter a user utterance, simply type it in the text field "
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
			+ "do not move left (0.15)<br><br></html>";

	public static Logger log = new Logger("ChatWindowTab", Logger.Level.DEBUG); 

	// main chat window
	HTMLEditorKit kit;
	HTMLDocument doc;

	// input field
	Container inputContainer;
	JTextField inputField;
	JTextPane lines;

	// last updated variable in the chat
	String lastUpdatedVariable = "";
	long lastUpdate = System.currentTimeMillis();
	int negativeOffset = 0;

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
		DefaultCaret caret = (DefaultCaret)lines.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		inputContainer = new Container();
		inputContainer.setLayout(new BorderLayout());

		inputField = new JTextField(60);		
		inputContainer.add(new JLabel("User input: "), BorderLayout.WEST);
		inputContainer.add(inputField, BorderLayout.CENTER);
		final JButton helpButton = new JButton( "" );
		helpButton.putClientProperty( "JButton.buttonType", "help" );

		final BalloonTip tip = new BalloonTip(helpButton, TIP_TEXT);
		tip.setVisible(false);

		helpButton.addActionListener(e -> tip.setVisible(!tip.isVisible()));
		helpButton.setFocusable(false);

		inputContainer.add(helpButton, BorderLayout.EAST);
		// Add the text field and the utterances
		add (inputContainer, BorderLayout.SOUTH);
		add (utterancesScrollPane, BorderLayout.CENTER);
		//	setPreferredSize(new Dimension(380,380));

		inputField.addActionListener(e -> addUtteranceToState());	

		kit = new HTMLEditorKit();
		doc = new HTMLDocument();
		lines.setEditorKit(kit);
		lines.setDocument(doc);
		lines.setFocusable(false);
		updateActivation();

		if (system.getModule(GUIFrame.class).isSpeechEnabled) {
			enableSpeech(true);
		}
	} 


	/**
	 * Enables or disables the speech input panel in the tab 
	 * 
	 * @param toEnable true if the speech panel should be enabled, else false
	 */
	public void enableSpeech(boolean toEnable) {
		if (inputContainer.getComponentCount() == 3 && toEnable) {
			SpeechInputPanel panel = new SpeechInputPanel(system);
			inputContainer.add(panel, BorderLayout.SOUTH);
			repaint();
		}
		else if (inputContainer.getComponentCount() == 4 && !toEnable) {
			inputContainer.remove(3);
			repaint();
		}
	}

	/**
	 * Sets the number of N-Best elements to display
	 * 
	 * @param nBestView the number of N-best elements to use
	 */
	public void setNBest(int nBestView) {
		this.nBestView = nBestView;
	}


	/**
	 * Returns the current number of displayed N-best elements
	 * 
	 * @return the number of N-best elements to display
	 */
	public int getNBest() {
		return nBestView;
	}


	/**
	 * Adds the utterance entered in the text field to the dialogue state
	 */
	private void addUtteranceToState()  {
		String rawText= inputField.getText();
		if (!rawText.equals("")) {
			String[] splitText = rawText.split(";");

			CategoricalTable table = new CategoricalTable(system.getSettings().userInput);

			for (String split : Arrays.asList(splitText)) {				
				Map.Entry<String, Float> split2 = getProbabilityValueInParenthesis(split);
				table.addRow(split2.getKey(), split2.getValue());
			}

			inputField.setText("");
			(new StateUpdater(system, table)).start();

		}
	}


	/**
	 * Adds a comment in the chat window
	 * 
	 * @param comment the comment to display
	 */
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


	/**
	 * Generates the HTML representation for the categorical table.
	 * 
	 * @param table the table
	 * @return the HTML rendering of the table
	 */
	private String getHtmlRendering(CategoricalTable table) {

		String htmlTable = "";
		String baseVar = table.getVariable().replace("'", "");
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
		for (Value value : table.getValues()) {
			if (!(value instanceof NoneVal)) {
				htmlTable += "<td><font size=4>";
				String content = value.toString();
				if (table.getProb(value) < 0.98) {
					content += " (" + StringUtils.getShortForm(table.getProb(value)) + ")";
				}
				if (system.getSettings().varsToMonitor.contains(baseVar)) {
					content = "<i>" + content + "</i>";
				}
				htmlTable += content + "</font></td></tr><tr><td></td>";
			}
		}
		htmlTable = htmlTable.substring(0, htmlTable.length() - 13) + "</table></p>\n";		

		return htmlTable;		
	}


	/**
	 * Updates the activation status of the chat window.
	 */
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


	/**
	 * Triggers the update of the chat window.  The window is updated whenever the
	 * updated variables contains the user input, system output, or other variables
	 * to monitor.
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the list of recently updated variables
	 */
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		updateActivation();
		if (updatedVars.contains(system.getSettings().userInput)
				&& state.hasChanceNode(system.getSettings().userInput)) {	
			CategoricalTable distrib = state.queryProb(
					system.getSettings().userInput, false).toDiscrete();
			showVariable(distrib);
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



	/**
	 * Displays the distribution in the chat window.
	 * 
	 * @param distrib the distribution to display
	 */
	private void showVariable(CategoricalTable distrib) {
		
		if (distrib.getBest() == ValueFactory.none()) {
			distrib = distrib.getNBest(nBestView+1);
		}
		else {
			distrib = distrib.getNBest(nBestView);
		}
		String text = getHtmlRendering(distrib);
		String variable = distrib.getVariable();
		try {
			if (variable.equals(lastUpdatedVariable) 
					&& (system.getState().isIncremental(variable))) {
				doc.remove(doc.getLength() - negativeOffset, negativeOffset);
			}
			int initLength = doc.getLength();
			kit.read(new StringReader(text), doc, doc.getLength());
			lastUpdatedVariable = variable;
			lastUpdate = System.currentTimeMillis();
			negativeOffset = doc.getLength() - initLength;
		}
		catch (Exception e) {
			log.warning("text area exception: " + e);
		}
	}


	/**
	 * Returns the current text of the chat.
	 * 
	 * @return the current text
	 */
	public String getChat() {
		return lines.getText();
	}


	/**
	 * Thread employed to update the dialogue state
	 */
	final class StateUpdater extends Thread {

		DialogueSystem system;
		CategoricalTable table;

		/**
		 * Constructs a new state updater
		 * @param system the dialogue system
		 * @param table the categorical table to insert
		 */
		public StateUpdater(DialogueSystem system, CategoricalTable table) {
			this.system = system;
			this.table = table;
		}

		/**
		 * Updates the dialogue state with the table
		 */
		@Override
		public void run() {
			try {
				system.addContent(table);

				// TODO: add a notation mechanism to add incremental content
				// (no need to document, internal functionality)
				// system.addIncrementalContent(table, true);
			}
			catch (DialException e) {
				log.warning("cannot update state with user utterance");
			}
		}
	}



}
