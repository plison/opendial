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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.gui.audio.SpeechInputPanel;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;


/**
 * GUI tab for the chat window.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
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
			+ "- To enter an N-best list of user utterances, separate each<br>"
			+ "&nbsp;&nbsp;&nbsp;alternative recognition hypothesis with a semicolon, as in:<br>"
			+ "<p style=\"font-size: 2px\">&nbsp;</p>&nbsp;&nbsp;&nbsp;<b>User input: </b><i>now move left (0.55) ; "
			+ "do not move left (0.15)</i><br><br>"
			+ "- Finally, to insert content other than user inputs into the dialogue state (for <br>"
			+ "&nbsp;&nbsp;&nbsp;instance, contextual variables), you can simply type into the text field:"
			+ "<p style=\"font-size: 2px\">&nbsp;</p>&nbsp;&nbsp;&nbsp;<i>var_name = the_content_to_add</i><br><br>"
			+ "&nbsp;&nbsp;&nbsp;where <i>var_name</i> is the variable label, and <i>the_content_to_add</i> its value(s),<br>"
			+ "&nbsp;&nbsp;&nbsp;using the same format as the one described above for user inputs.<br><br></html>";

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
		inputContainer.add(new JLabel("Input: "), BorderLayout.WEST);
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
		List<Value> rankedValues = table.getValues().stream()
				.sorted((v1,v2) -> Double.compare(table.getProb(v2), table.getProb(v1)))
				.collect(Collectors.toList());
		for (Value value : rankedValues) {
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
			if (lines.getText().length() <= 100 
					&& !lines.getText().contains("No domain currently selected")) {
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
	 * Adds the utterance entered in the text field to the dialogue state
	 * 
	 * NB: if the text is starting or ending with '/', assume it represents
	 * incremental inputs (where an '/' at the end represents an unfinished
	 * input, and '/' at the beginning a follow-up to a previous unit).
	 */
	private void addUtteranceToState()  {
		String rawText= inputField.getText().trim();
		inputField.setText("");
		if (rawText.equals("")) {
			return;
		}

		// special case : incremental user input
		else if (rawText.contains("/")) {
			addIncrementalUtterance(rawText);
		}

		// special case: input for custom variable (not user input)
		else if (rawText.contains("=")) {
			addSpecialInput(rawText);
		}
		
		// default case
		else {
			Map<String,Double> table = StringUtils.getTableFromInput(rawText);
			new Thread(() -> system.addUserInput(table)).start();
		}
	}

	/**
	 * Addition of an incremental user input to the dialogue state
	 * 
	 * @param rawText the raw text from the GUI
	 */
	private void addIncrementalUtterance(String rawText) {

		boolean followPrevious = rawText.startsWith("/");
		boolean incomplete = rawText.endsWith("/");
		rawText = rawText.replaceAll("/", "").trim();

		Map<String,Double> table = StringUtils.getTableFromInput(rawText);
		new Thread(() -> {
			system.addContent(new Assignment(system.getSettings().userSpeech, 
					(incomplete)? "busy": "None"));
			system.addIncrementalUserInput(table, followPrevious);
			if (!incomplete) {
				system.getState().setAsCommitted(system.getSettings().userInput);
			}
		}).start();
		
	}

	/**
	 * Adds a special input to the dialogue state (not the default user input)
	 * 
	 * @param rawText the raw text from the GUI
	 */
	private void addSpecialInput(String rawText) {
		String specialInput = rawText.split("=")[0].trim();
		rawText = rawText.split("=")[1].trim();

		Map<String,Double> table = StringUtils.getTableFromInput(rawText);

		CategoricalTable table2 = new CategoricalTable(specialInput);
		for (String value : table.keySet()) {
			table2.addRow(value, table.get(value));
		}
		new Thread(() -> system.addContent(table2)).start();
	}


}
