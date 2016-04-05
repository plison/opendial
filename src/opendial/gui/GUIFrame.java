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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.domains.Domain;
import opendial.modules.DialogueImporter;
import opendial.modules.DialogueRecorder;
import opendial.modules.Module;
import opendial.readers.XMLDomainReader;
import opendial.utils.XMLUtils;

/**
 * Main GUI frame for the OpenDial toolkit, encompassing various tabs and menus to
 * control the application
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class GUIFrame implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String ICON_PATH = "resources/opendial-icon.png";

	JFrame frame;

	// tabbed pane
	JTabbedPane tabbedPane;

	// tab for the state monitor
	StateMonitorTab stateMonitorTab;

	// tab for the chat window
	InteractionTab chatTab;

	// tab for the domain editor
	EditorTab editorTab;

	DialogueSystem system;

	GUIMenuBar menu;

	boolean isSpeechEnabled = false;

	// ===================================
	// GUI CONSTRUCTION
	// ===================================

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
				File f = new File(ICON_PATH);
				if (f.exists()) {
					frame.setIconImage(ImageIO.read(f));
				}
				else {
					frame.setIconImage(
							ImageIO.read(GUIFrame.class.getResourceAsStream(
									"/" + ICON_PATH.replace("//", "/"))));
				}
			}
			catch (Exception e) {
				log.fine("could not employ icon: " + e);
			}
			tabbedPane = new JTabbedPane();
			frame.getContentPane().add(tabbedPane);

			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					if (isDomainSaved() || requestSave()) {
						System.exit(0);
					}
				}
			});

			tabbedPane.addMouseListener(new ClickListener());

			frame.setLocation(new Point(200, 200));

			menu = new GUIMenuBar(this);
			frame.setJMenuBar(menu);

			chatTab = new InteractionTab(system);
			tabbedPane.addTab(InteractionTab.TAB_TITLE, null, chatTab,
					InteractionTab.TAB_TIP);

			stateMonitorTab = new StateMonitorTab(this);
			tabbedPane.addTab(StateMonitorTab.TAB_TITLE, null, stateMonitorTab,
					StateMonitorTab.TAB_TIP);

			editorTab = new EditorTab(this);
			tabbedPane.addTab(EditorTab.TAB_TITLE, null, editorTab,
					EditorTab.TAB_TIP);

			frame.setPreferredSize(new Dimension(900, 800));
			frame.pack();

			frame.setVisible(true);
		}
		refresh();
	}

	// ===================================
	// GUI UPDATE
	// ===================================

	/**
	 * Pauses the GUI.
	 */
	@Override
	public void pause(boolean pause) {
		if (frame != null && frame.isVisible()) {
			chatTab.refresh();
		}
	}

	/**
	 * Updates the current dialogue state displayed in the component. The current
	 * dialogue state is name "current" in the selection list.
	 * 
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (frame != null && frame.isVisible()) {
			chatTab.trigger(state, updatedVars);
			stateMonitorTab.refresh(state, updatedVars);
		}
		refresh();
	}

	/**
	 * Refreshes the GUI (menu, title and domain content).
	 */
	public void refresh() {
		if (frame != null && frame.isVisible()) {
			menu.update();
			String title = "OpenDial toolkit";
			if (!system.getDomain().isEmpty()) {
				title += " - domain: "
						+ system.getDomain().getSourceFile().getName();
				editorTab.refresh();
			}
			else {
				title += " (no domain)";
			}
			if (!frame.getTitle().equals(title)) {
				frame.setTitle(title);
			}
			chatTab.refresh();
		}
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
		if (frame != null) {
			stateMonitorTab.recordState(state, name);
		}
	}

	/**
	 * Adds a comment to the chat window
	 * 
	 * @param comment the comment to add
	 */
	public void addComment(String comment) {
		if (frame != null) {
			chatTab.addComment(comment);
		}
		if (tabbedPane.getSelectedIndex() == 2) {
			editorTab.displayComment(comment);
		}
	}

	/**
	 * Enables or disables the speech recording functionality in the GUI
	 * 
	 * @param toEnable true if the speech functionality should be enabled, else
	 *            false.
	 */
	public void enableSpeech(boolean toEnable) {
		isSpeechEnabled = toEnable;
		if (chatTab != null) {
			chatTab.enableSpeech(toEnable);
		}
		if (menu != null) {
			menu.enableSpeech(toEnable);
		}
	}

	/**
	 * If isSaved is false, sets a '*' star on the editor tab to mark the fact that
	 * the domain has been modified without being saved. If isSaved is true, removes
	 * the '*' star if there was one.
	 * 
	 * @param isSaved whether the domain has been saved
	 */
	protected void setSavedFlag(boolean isSaved) {
		if (isSaved && !isDomainSaved()) {
			tabbedPane.setTitleAt(2, EditorTab.TAB_TITLE);
			menu.update();
		}
		else if (!isSaved && isDomainSaved()) {
			tabbedPane.setTitleAt(2, EditorTab.TAB_TITLE + "*");
			menu.update();
		}
	}

	/**
	 * Changes the active tab in the GUI.
	 * 
	 * @param i the index (0 for the interaction tab, 1 for the state monitor, 2 for
	 *            the domain editor).
	 */
	public void setActiveTab(int i) {
		if (i >= 0 && i <= 2) {
			tabbedPane.setSelectedIndex(i);
		}
		else {
			log.warning("Cannot activate tab (out-of-bounds index)");
		}
	}

	/**
	 * Asks the user whether to save the file. The method returns true if an action
	 * (save or discard changes) has been performed. Else (i.e. if the user has
	 * clicked cancel), return false.
	 * 
	 * @return true if yes/no to save, false if cancel.
	 */
	private boolean requestSave() {
		String msg = "Save edited domain file?";
		int n = JOptionPane.showConfirmDialog(frame, msg);
		if (n == 0) {
			saveDomain();
			return true;
		}
		else if (n == 1) {
			editorTab.rereadFile();
			setSavedFlag(true);
			return true;
		}
		return false;
	}

	/**
	 * Closes the window (and OpenDial).
	 */
	public void closeWindow() {
		WindowEvent ev = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
		frame.dispatchEvent(ev);
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the dialogue system connected to the GUI
	 * 
	 * @return the dialogue system
	 */
	public DialogueSystem getSystem() {
		return system;
	}

	/**
	 * Returns the chat tab
	 * 
	 * @return the chat tab
	 */
	public InteractionTab getChatTab() {
		return chatTab;
	}

	/**
	 * Returns the state viewer tab
	 * 
	 * @return the state viewer
	 */
	public StateMonitorTab getStateViewerTab() {
		return stateMonitorTab;
	}

	/**
	 * Returns the editor tab.
	 * 
	 * @return the editor tab
	 */
	public EditorTab getEditorTab() {
		return editorTab;
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

	/**
	 * Returns whether the speech recording functionality if enabled in the GUI
	 * 
	 * @return true if the speech recording function is activated, else false.
	 */
	public boolean isSpeechEnabled() {
		return isSpeechEnabled;
	}

	/**
	 * Returns true if the domain in the domain specification in the editor pane has
	 * been saved (or has not been modified), and false otherwise
	 * 
	 * @return true if the domain is saved, false otherwise
	 */
	public boolean isDomainSaved() {
		return !tabbedPane.getTitleAt(2).contains("*");
	}

	/**
	 * Returns the menu bar for the frame
	 * 
	 * @return the menu bar
	 */
	public GUIMenuBar getMenu() {
		return menu;
	}

	// ===================================
	// I/O OPERATIONS
	// ===================================

	/**
	 * Creates a new domain
	 */
	public void newDomain() {
		JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
		fileChooser.setDialogTitle("Save the new domain in file ...");
		fileChooser.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));

		if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			if (fileToSave.exists()) {
				int result = JOptionPane.showConfirmDialog(frame,
						"The file exists, overwrite?", "Existing file",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (result == JOptionPane.NO_OPTION
						|| result == JOptionPane.CLOSED_OPTION
						|| result == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			newDomain(fileToSave);
		}
	}

	/**
	 * Creates a new domain and saves it in the file given as argument.
	 * 
	 * @param fileToSave the file in which to save the domain
	 */
	public void newDomain(File fileToSave) {

		try {
			String skeletton = "<domain>\n\n</domain>";
			Files.write(Paths.get(fileToSave.toURI()), skeletton.getBytes());
			log.info("Saving domain in " + fileToSave);
			Domain newDomain =
					XMLDomainReader.extractDomain(fileToSave.getAbsolutePath());
			system.changeDomain(newDomain);
			refresh();
			system.displayComment("Dialogue domain successfully created");
		}
		catch (IOException e) {
			log.severe("Cannot create new domain: " + e);
		}
	}

	/**
	 * Opens an existing dialogue domain.
	 */
	protected void openDomain() {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String domainFile = fc.getSelectedFile().getAbsolutePath();
			try {
				Domain domain = XMLDomainReader.extractDomain(domainFile);
				system.changeDomain(domain);
				refresh();
			}
			catch (RuntimeException j) {
				addComment("Cannot use domain: " + j);
				Domain dummy = XMLDomainReader.extractEmptyDomain(domainFile);
				system.changeDomain(dummy);
				refresh();
			}
		}
	}

	/**
	 * Saves the dialogue domain specification to the current file
	 * 
	 */
	public void saveDomain() {
		saveDomain(editorTab.getShownFile());
		system.refreshDomain();
	}

	/**
	 * Saves the dialogue domain specification to a new file
	 * 
	 */
	protected void saveDomainAs() {

		Domain domain = system.getDomain();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(domain.getSourceFile().getParentFile());
		fileChooser.setDialogTitle("Save the domain in file ...");
		fileChooser.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));

		if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			log.info("Saving domain in " + domain.getSourceFile().getName());
			saveDomain(fileToSave);
			if (editorTab.getShownFile().equals(domain.getSourceFile())) {
				domain.setSourceFile(fileToSave);
			}
			system.displayComment("Dialogue domain saved in " + fileToSave);
			system.refreshDomain();
		}
	}

	/**
	 * Saves the dialogue domain in the editor tab to the file given as argument
	 * 
	 * @param fileToWrite the file in which to write the domain.
	 */
	public void saveDomain(File fileToWrite) {
		String curText = editorTab.getText();
		if (fileToWrite != null) {
			try {
				Files.write(Paths.get(fileToWrite.toURI()), curText.getBytes());
			}
			catch (IOException e) {
				log.severe("Cannot save domain: " + e);
				e.printStackTrace();
				editorTab.rereadFile();
			}
			setSavedFlag(true);
			refresh();
		}
	}

	/**
	 * Resets the interaction (resetting the dialogue state to its initial value).
	 */
	public void resetInteraction() {
		chatTab.reset();
		addComment("Reinitialising interaction...");
		system.changeDomain(system.getDomain());
		refresh();
		stateMonitorTab.reset(system.getState());
	}

	/**
	 * Imports a previous interaction.
	 * 
	 * @param isWizardOfOz whether the interaction is a WOZ study.
	 */
	protected void importInteraction(boolean isWizardOfOz) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String interactionFile = fc.getSelectedFile().getAbsolutePath();
			addComment("Importing interaction " + interactionFile);
			try {
				DialogueImporter importer = system.importDialogue(interactionFile);
				importer.setWizardOfOzMode(isWizardOfOz);
			}
			catch (Exception f) {
				log.warning("could not extract interaction: " + f);
				addComment(f.toString());
			}
		}
	}

	/**
	 * Records the interaction.
	 */
	protected void saveInteraction() {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String recordFile = fc.getSelectedFile().getAbsolutePath();
			system.getModule(DialogueRecorder.class).writeToFile(recordFile);
			addComment("Interaction saved to " + recordFile);
		}
	}

	/**
	 * Imports a dialogue state or prior parameter distributions.
	 * 
	 * @param tag the expected top XML tag.
	 */
	protected void importContent(String tag) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String stateFile = fc.getSelectedFile().getAbsolutePath();
			addComment("Importing " + tag + " from " + stateFile);
			try {
				XMLUtils.importContent(system, stateFile, tag);
			}
			catch (Exception f) {
				log.warning("could not extract interaction: " + f);
				addComment(f.toString());
			}
		}
	}

	/**
	 * Exports a dialogue state or prior parameter distributions.
	 * 
	 * @param tag the expected top XML tag.
	 */
	protected void exportContent(String tag) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String recordFile = fc.getSelectedFile().getAbsolutePath();
				XMLUtils.exportContent(system, recordFile, tag);
				addComment(tag.substring(0, 1).toUpperCase() + tag.substring(1)
						+ " saved to " + recordFile);
			}
			catch (RuntimeException j) {
				log.warning("could not save parameter distribution: " + j);
			}
		}
	}

	/**
	 * Listener for clicks on the tabs. If the domain editor contains unsaved
	 * content, asks the user whether to save them or not.
	 *
	 */
	final class ClickListener extends MouseAdapter implements MouseListener {

		@Override
		public void mousePressed(MouseEvent e) {
			if (!isDomainSaved() && requestSave()) {
				e = new MouseEvent(e.getComponent(), MouseEvent.MOUSE_RELEASED,
						e.getWhen() + 100, e.getModifiers(), e.getX(), e.getY(), 1,
						false);
				e.getComponent().dispatchEvent(e);
			}
		}

	}

}
