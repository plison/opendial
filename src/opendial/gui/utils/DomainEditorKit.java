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

package opendial.gui.utils;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Editor kit for the XML domain specification. The kit provides syntax highlighting,
 * auto-indentation, and auto-completion.
 * 
 * <p>
 * Credits: part of this code are refactored from the 'bounce' XML editor kit.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class DomainEditorKit extends StyledEditorKit implements KeyListener {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	private static final long serialVersionUID = 2969169649596107757L;

	// view factory
	private ViewFactory xmlViewFactory;

	// ===================================
	// KIT CONSTRUCTION
	// ===================================

	/**
	 * Creates a new editor kit with an associated factory
	 */
	public DomainEditorKit() {
		xmlViewFactory = new XmlViewFactory();
	}

	/**
	 * Returns the view factory
	 */
	@Override
	public ViewFactory getViewFactory() {
		return xmlViewFactory;
	}

	/**
	 * Returns 'text/xml'.
	 */
	@Override
	public String getContentType() {
		return "text/xml";
	}

	/**
	 * Installs the editor for the kit.
	 */
	@Override
	public void install(JEditorPane editor) {
		super.install(editor);
		editor.addKeyListener(this);
	}

	/**
	 * Deinstalls the editor for the kit.
	 */
	@Override
	public void deinstall(JEditorPane editor) {
		super.deinstall(editor);
		editor.removeKeyListener(this);
	}

	// ===================================
	// EVENT HANDLING: AUTO-COMPLETIONS
	// ===================================

	/**
	 * Reacts to typed keys (closing brackets and empty spaces).
	 */
	@Override
	public void keyTyped(KeyEvent event) {

		try {
			if (event.getKeyChar() == '>') {
				onBraceClosed(event);
			}
			else if (event.getKeyChar() == ' ') {
				onEntityOpened(event);
			}
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Does nothing
	 */
	@Override
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * Reacts to a closing brace. If the braces closes a valid XML entity, attempts
	 * to auto-complete the tag. If the tag is a 'rule' or a 'case', also constructs
	 * a minimal skeleton for the entity.
	 * 
	 * @param event the key event to handle (must be a typed '>')
	 * @throws BadLocationException
	 */
	private static void onBraceClosed(KeyEvent event) throws BadLocationException {

		JEditorPane editor = (JEditorPane) event.getSource();
		Document doc = editor.getDocument();
		int pos = editor.getCaretPosition();

		String tag = getCurrentTag(doc.getText(0, pos));
		if (tag.equals("")) {
			return;
		}

		// checking that the tag is not already closed
		String textAfter = doc.getText(pos, doc.getLength() - pos);
		Matcher m = Pattern.compile("<" + tag).matcher(textAfter);
		Matcher m2 = Pattern.compile("</" + tag).matcher(textAfter);
		int openingCount = 0;
		while (m.find()) {
			openingCount++;
		}
		int closingCount = 0;
		while (m2.find()) {
			closingCount++;
		}
		if (openingCount == closingCount - 1) {
			return;
		}

		StringBuffer buffer = new StringBuffer();
		int newPosition = pos;
		String indent = getIndent(doc, pos);
		if (tag.equals("rule")) {
			buffer.append(">\n" + indent + "\t<case>\n");
			buffer.append(indent + "\t\t<condition>\n");
			buffer.append(
					indent + "\t\t\t<if var=\"\" relation=\"=\" value=\"\"/>\n");
			buffer.append(indent + "\t\t</condition>\n");
			String param = (isUtility(doc, pos)) ? "util=\"0\"" : "prob=\"1\"";
			buffer.append(indent + "\t\t<effect " + param + ">\n");
			buffer.append(indent + "\t\t\t<set var=\"\" value=\"\"/>\n");
			buffer.append(indent + "\t\t</effect>\n");
			buffer.append(indent + "\t</case>\n");
			buffer.append(indent + "</rule>\n");
			newPosition += 36 + indent.length() * 3;
		}
		else if (tag.equals("case")) {
			buffer.append(">\n" + indent + "\t<condition>\n");
			buffer.append(indent + "\t\t<if var=\"\" relation=\"=\" value=\"\"/>\n");
			buffer.append(indent + "\t</condition>\n");
			String param = (isUtility(doc, pos)) ? "util=\"0\"" : "prob=\"1\"";
			buffer.append(indent + "\t<effect " + param + ">\n");
			buffer.append(indent + "\t\t<set var=\"\" value=\"\"/>\n");
			buffer.append(indent + "\t</effect>\n");
			buffer.append(indent + "</case>");
			newPosition += 26 + indent.length() * 2;
		}
		else {
			buffer.append("></" + tag + ">");
			newPosition += 1;
		}
		doc.insertString(pos, buffer.toString(), null);
		editor.setCaretPosition(newPosition);
		event.consume();

	}

	/**
	 * Reacts to an empty space event. If the space is typed within a tag that is a
	 * 'if', 'set', 'value', 'variable', or 'effect', auto-completes the entity
	 * structure.
	 * 
	 * @param event the key event (must be a typed ' ' )
	 * @throws BadLocationException
	 */
	private static void onEntityOpened(KeyEvent event) throws BadLocationException {

		JEditorPane editor = (JEditorPane) event.getSource();
		Document doc = editor.getDocument();
		int pos = editor.getCaretPosition();

		String tag = getCurrentTag(doc.getText(0, pos));
		if (tag.equals("")) {
			return;
		}

		String remainder = doc.getText(pos, doc.getLength() - pos);
		for (int i = 0; i < remainder.length(); i++) {
			char c = remainder.charAt(i);
			if (c == '\n') {
				break;
			}
			else if (!Character.isWhitespace(c)) {
				return;
			}
		}

		StringBuffer buffer = new StringBuffer();
		int newPosition = pos;
		String indent = getIndent(doc, pos);
		if (tag.equals("if")) {
			buffer.append(" var=\"\" relation=\"=\" value=\"\"/>");
			newPosition += 6;
		}
		else if (tag.equals("set")) {
			buffer.append(" var=\"\" value=\"\"/>");
			newPosition += 6;
		}
		else if (tag.equals("value")) {
			buffer.append(" prob=\"\"></value>");
			newPosition += 7;
		}
		else if (tag.equals("variable")) {
			buffer.append(" id=\"\">\n");
			buffer.append(indent + "\t<value prob=\"1\"></value>\n");
			buffer.append(indent + "</variable>\n");
			newPosition += 5;
		}
		else if (tag.equals("model")) {
			buffer.append(" trigger=\"\">\n\n");
			buffer.append(indent + "</model>");
			newPosition += 10;
		}
		else if (tag.equals("effect")) {
			buffer.append(" prob=\"1\">\n");
			buffer.append(indent + "\t<set var=\"\" value=\"\"/>\n");
			buffer.append(indent + "</effect>\n");
			newPosition += 22 + indent.length();
		}
		else {
			return;
		}
		doc.insertString(pos, buffer.toString(), null);
		editor.setCaretPosition(newPosition);
		event.consume();
	}

	// ===================================
	// EVENT HANDLING: TAB AND RETURNS
	// ===================================

	/**
	 * Reacts to pressed keys (returns and tabs).
	 */
	@Override
	public void keyPressed(KeyEvent event) {

		try {
			if (event.getKeyChar() == '\n') {
				onReturnPressed(event);
			}
			else if (event.getKeyChar() == '\t') {
				onTabPressed(event);
			}

		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reacts to a pressed tab. If the tab happens upon filling an attribute value
	 * and there are other empty attribute values in the editor, automatically goes
	 * to the start of the next attribute value. Else, does nothing.
	 * 
	 * @param event the key event to handle (must be a pressed '\t').
	 * @throws BadLocationException
	 */
	private static void onTabPressed(KeyEvent event) throws BadLocationException {

		JEditorPane editor = (JEditorPane) event.getSource();
		Document doc = editor.getDocument();
		int pos = editor.getCaretPosition();

		String remainder = doc.getText(pos, doc.getLength() - pos);
		if (pos < doc.getLength() - 1 && remainder.charAt(0) == '"') {

			for (int i = 1; i < remainder.length() - 1; i++) {
				char c = remainder.charAt(i);
				char c2 = remainder.charAt(i + 1);
				if (c == '"' && c2 == '"') {
					editor.setCaretPosition(pos + i + 1);
					event.consume();
					return;
				}
			}
		}
	}

	/**
	 * Reacts to a pressed return. Simply goes to the next line with the proper
	 * indentation.
	 * 
	 * @param event the key event to handle (must be a pressed '\n').
	 * @throws BadLocationException
	 */
	private static void onReturnPressed(KeyEvent event) throws BadLocationException {
		JEditorPane editor = (JEditorPane) event.getSource();
		Document doc = editor.getDocument();
		int pos = editor.getCaretPosition();
		String indent = getIndent(doc, pos);

		Element root = doc.getDefaultRootElement();
		Element elem = root.getElement(root.getElementIndex(pos));
		int start = elem.getStartOffset();
		String line = doc.getText(start, pos - start);

		if (isStartElement(line)) {
			indent += "\t";
		}
		doc.insertString(pos, "\n" + indent, null);
		event.consume();
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns the current (last) tag in the text. The tag must be opened but not yet
	 * closed. If the tag cannot be found, returns a empty string.
	 * 
	 * @param text the text containing the open tag
	 * @return the resulting tag, or an empty string.
	 */
	private static String getCurrentTag(String text) {

		int opening = text.lastIndexOf('<');
		int closing = text.lastIndexOf('>');
		if (opening < 0 || closing > opening) {
			return "";
		}
		String entity = text.substring(opening, text.length());

		if (entity.length() <= 1) {
			return "";
		}
		char first = entity.charAt(1);
		char previous = entity.charAt(entity.length() - 1);
		if (first != '/' && first != '!' && first != '?'
				&& !Character.isWhitespace(first) && previous != '/'
				&& previous != '-') {
			StringBuffer tag = new StringBuffer();
			for (int i = 1; i < entity.length(); i++) {
				char ch = entity.charAt(i);
				if (Character.isWhitespace(ch)) {
					break;
				}
				tag.append(ch);
			}
			return tag.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Returns the indent at the current position.
	 * 
	 * @param doc the document
	 * @param position the caret position
	 * @return the indent string
	 * @throws BadLocationException
	 */
	private static String getIndent(Document doc, int position)
			throws BadLocationException {

		Element root = doc.getDefaultRootElement();
		Element elem = root.getElement(root.getElementIndex(position));
		int start = elem.getStartOffset();
		String line = doc.getText(start, position - start);
		StringBuffer newStr = new StringBuffer();
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '\n' || ch == 'f' || ch == '\r'
					|| !Character.isWhitespace(ch)) {
				break;
			}
			newStr.append(ch);
		}

		return newStr.toString();
	}

	/**
	 * Returns true if the caret is currently in a model that contains a majority of
	 * utility rules. Otherwise, returns false.
	 * 
	 * @param doc the document
	 * @param position the caret position
	 * @return true if there are more utility rules than prob rules, else false
	 * @throws BadLocationException
	 */
	private static boolean isUtility(Document doc, int position)
			throws BadLocationException {

		String before = doc.getText(0, position);
		String after = doc.getText(position, doc.getLength() - position);
		int start = before.lastIndexOf("<model");
		int middle = before.lastIndexOf("</model>");
		int end = after.indexOf("</model>");

		if (start >= 0 && end >= 0 && middle < start) {
			String modeltext = before.substring(start, before.length())
					+ after.substring(0, end);
			int countProbs = 0;
			int countUtils = 0;
			Pattern p = Pattern.compile("((?:prob)|(?:util))\\=");
			Matcher m = p.matcher(modeltext);
			while (m.find()) {
				if (m.group(1).equals("prob")) {
					countProbs++;
				}
				else {
					countUtils++;
				}
			}
			return countUtils > countProbs;
		}
		return false;
	}

	/**
	 * Tries to find out if the line finishes with an element start
	 * 
	 * @param line the line to process
	 * @return true if the line finishes with a new entity
	 */
	private static boolean isStartElement(String line) {

		int first = line.lastIndexOf("<");
		int last = line.lastIndexOf(">");

		if (last < first) {
			return true;
		}
		else {
			int firstEnd = line.lastIndexOf("</");
			int lastEnd = line.lastIndexOf("/>");

			if ((firstEnd != first) && ((lastEnd + 1) != last)) {
				return true;
			}
		}
		return false;
	}

	// ===================================
	// HELPER CLASS
	// ===================================

	/**
	 * Basic view factory for the domain editor kit.
	 *
	 */
	final class XmlViewFactory implements ViewFactory {

		/**
		 * @see javax.swing.text.ViewFactory#create(javax.swing.text.Element)
		 */
		@Override
		public View create(Element element) {

			return new XMLView(element);
		}

	}

}