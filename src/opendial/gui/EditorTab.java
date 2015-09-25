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
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.RoundedBalloonStyle;
import opendial.domains.Domain;
import opendial.gui.utils.DomainEditorKit;

public class EditorTab extends JComponent {

	private static final long serialVersionUID = 1L;

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static final String TAB_TITLE = " Domain editor ";
	public static final String TAB_TIP = "User-friendly editor for dialogue domains";

	// the frame including the tab
	private GUIFrame frame;

	// list of XML source files
	private DomainFilesModel listModel;

	// file (to be) shown on screen
	private File shownFile;

	// the domain editor
	public JEditorPane editor;

	// ===================================
	// EDITOR CONSTRUCTION
	// ===================================

	/**
	 * Creates the editor tab inside the given frame. The tab contains a small column
	 * with the XML files associated with the domain. At the center of the window is
	 * an editor panel to modify the domain.
	 * 
	 * @param frame the GUI frame
	 */
	public EditorTab(GUIFrame frame) {
		setLayout(new BorderLayout());

		this.frame = frame;

		listModel = new DomainFilesModel();

		JList<String> listBox = new JList<String>(listModel);
		listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListListener listener = new ListListener();
		listBox.addListSelectionListener(listener);
		listBox.setBorder(BorderFactory.createTitledBorder("XML File(s):"));
		listBox.addMouseListener(frame.new ClickListener());

		editor = new JEditorPane();
		// Instantiate a XMLEditorKit
		DomainEditorKit kit = new DomainEditorKit();
		editor.setEditorKit(kit);

		JScrollPane scroller = new JScrollPane(editor);

		JSplitPane topPanel =
				new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listBox, scroller);
		topPanel.setDividerLocation(200);

		// Create undo/redo actions and add it to the text component
		Action undoredo = new UndoRedoAction();
		editor.getActionMap().put("Undo", undoredo);
		editor.getActionMap().put("Redo", undoredo);

		// Bind the undo and redo actions
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask);
		KeyStroke redoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask);
		editor.getInputMap().put(undoKey, "Undo");
		editor.getInputMap().put(redoKey, "Redo");

		add(topPanel);
		refresh();

	}

	// ===================================
	// EDITOR UPDATE
	// ===================================

	/**
	 * Refreshes the dialogue domain shown in the tab
	 */
	public void refresh() {
		Domain updatedDomain = frame.getSystem().getDomain();
		if (updatedDomain.isEmpty()) {
			editor.setEnabled(false);
			editor.setToolTipText("No dialogue domain is selected");
		}
		else {
			editor.setEnabled(true);
			editor.setToolTipText(null);
		}
		if (listModel.isChanged(updatedDomain)) {
			listModel.updateDomain(updatedDomain);
			if (!listModel.isEmpty() && !listModel.containsFile(shownFile)) {
				shownFile = updatedDomain.getSourceFile();
				rereadFile();
			}
		}
	}

	/**
	 * Displays a comment as a balloon tip.
	 * 
	 * @param msg the message to display
	 */
	public void displayComment(String msg) {
		Color msgColor = (msg.contains("error")) ? new Color(250, 230, 230)
				: new Color(230, 250, 230);
		BalloonTipStyle style = new RoundedBalloonStyle(5, 5, msgColor, Color.BLACK);
		BalloonTip tip = new BalloonTip(frame.getMenu(), msg, style, false);
		tip.setVisible(true);
		new Thread(() -> {
			try {
				Thread.sleep(2000);
			}
			catch (Exception e) {
			}
			tip.closeBalloon();
		}).start();
	}

	/**
	 * Reads the xml file in the object variable 'shownFile' and displays its content
	 * in the editor pane.
	 */
	protected void rereadFile() {
		try {
			editor.read(new FileReader(shownFile), shownFile);

			// Set the font style (default 13).
			editor.setFont(new Font("Verdana", Font.PLAIN, 13));

			// Set the tab size
			editor.getDocument().putProperty(PlainDocument.tabSizeAttribute,
					new Integer(2));

			Document doc = editor.getDocument();
			doc.addDocumentListener(new XMLListener());
			doc.addUndoableEditListener(
					(UndoRedoAction) editor.getActionMap().get("Undo"));
			editor.requestFocus();
			editor.requestFocusInWindow();

		}
		catch (IOException e) {
			log.severe("cannot read xml file: " + e);
		}
	}

	/**
	 * Sets the text in the editor (erasing the previous content).
	 * 
	 * @param text the text to write in the editor.
	 */
	public void setText(String text) {
		Document doc = editor.getEditorKit().createDefaultDocument();
		editor.setDocument(doc);
		editor.setText(text);
		frame.setSavedFlag(false);
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the text currently in the editor pane.
	 * 
	 * @return the text
	 */
	public String getText() {
		return editor.getText();
	}

	/**
	 * Returns the file currently shown in the editor pane.
	 * 
	 * @return the file object
	 */
	public File getShownFile() {
		return shownFile;
	}

	/**
	 * Returns the files in the left column of the editor tab.
	 * 
	 * @return the list of available domain files.
	 */
	public List<File> getFiles() {
		return new ArrayList<File>(listModel.xmlFiles.values());
	}

	// ===================================
	// HELPER CLASSES
	// ===================================

	/**
	 * List model containing the domain files (in the left column of the editor tab).
	 *
	 */
	@SuppressWarnings("serial")
	final class DomainFilesModel extends DefaultListModel<String> {

		// the xml files
		Map<String, File> xmlFiles;

		/**
		 * Creates a empty model
		 */
		public DomainFilesModel() {
			super();
			xmlFiles = new LinkedHashMap<String, File>();
		}

		/**
		 * Returns true if the model contains the given file, and false otherwise.
		 * 
		 * @param file the file to check
		 * @return true if the model contains the file, else false
		 */
		public boolean containsFile(File file) {
			return (file == null) ? false : xmlFiles.containsKey(file.getName());
		}

		/**
		 * Returns true if the model needs to be updated given the provided domain,
		 * and false otherwise
		 * 
		 * @param domain the (possibly updated) domain
		 * @return true if an update is necessary, false otherwise
		 */
		public boolean isChanged(Domain domain) {
			return !xmlFiles.values().contains(domain.getSourceFile())
					|| !xmlFiles.values().containsAll(domain.getImportedFiles())
					|| xmlFiles.size() != domain.getImportedFiles().size() + 1;
		}

		/**
		 * Updates the model with the updated domain
		 * 
		 * @param domain the dialogue domain
		 */
		public void updateDomain(Domain domain) {
			xmlFiles.clear();
			super.removeAllElements();
			if (!domain.isEmpty()) {
				File srcFile = domain.getSourceFile();
				xmlFiles.put(srcFile.getName(), srcFile);
				for (File importedFile : domain.getImportedFiles()) {
					xmlFiles.put(importedFile.getName(), importedFile);
				}
				xmlFiles.keySet().stream().forEach(f -> addElement(f));
			}
		}

		/**
		 * Returns the file at the given index in the domain
		 * 
		 * @param index the index in the list
		 * @return the corresponding file
		 */
		public File getFileAt(int index) {
			String filename = super.getElementAt(index);
			if (xmlFiles.containsKey(filename)) {
				return xmlFiles.get(filename);
			}
			throw new RuntimeException("file not found in model: " + filename);
		}

	}

	/**
	 * Action class to handle undo/redo operations in the editor pane
	 *
	 */
	@SuppressWarnings("serial")
	final class UndoRedoAction extends AbstractAction
			implements UndoableEditListener {

		// the undo manager
		UndoManager undo;

		/**
		 * Creates the action (and an undo manager)
		 */
		public UndoRedoAction() {
			super();
			undo = new UndoManager();
		}

		/**
		 * Performs an undo if an undo command was executed, and a redo if a redo
		 * command was executed
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (e.getActionCommand().equals("z")) {
					if (undo != null && undo.canUndo()) {
						undo.undo();
					}
				}
				else if (e.getActionCommand().equals("y")) {
					if (undo != null && undo.canRedo()) {
						undo.redo();
					}
				}
			}
			catch (CannotUndoException | CannotRedoException ex) {
			}
		}

		/**
		 * Performs the edit
		 */
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
		}

	}

	/**
	 * Listener for the list of XML files. Refreshes the editor pane with the
	 * selected file
	 *
	 */
	final class ListListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			@SuppressWarnings("unchecked")
			JList<String> jl = (JList<String>) e.getSource();

			int selected = jl.getSelectedIndex();
			if (selected >= 0 && !e.getValueIsAdjusting()) {
				shownFile = listModel.getFileAt(selected);
				rereadFile();
			}
		}
	}

	/**
	 * Listener for the document. Set the flag as unsaved if the domain is being
	 * modified.
	 */
	class XMLListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			frame.setSavedFlag(false);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			frame.setSavedFlag(false);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
		}

	}

}
