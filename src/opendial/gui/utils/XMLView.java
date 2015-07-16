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

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

/**
 * XML view for the domain editor kit.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLView extends PlainView {

	final static Logger log = Logger.getLogger("OpenDial");

	/** Patterns to look for in the text */
	private static HashMap<Pattern, Color> patternColors;
	public static Pattern TAG_PATTERN = Pattern.compile("(</?[a-z]*)\\s?>?");
	public static Pattern TAG_END_PATTERN = Pattern.compile("(/>)");
	public static Pattern TAG_ATTRIBUTE_PATTERN = Pattern.compile("\\s(\\w*)\\=");
	public static Pattern TAG_ATTRIBUTE_VALUE =
			Pattern.compile("[a-z-]*\\=(\"[^\"]*\")");
	public static Pattern TAG_COMMENT = Pattern.compile("(<!--.*-->)");
	public static Pattern TAG_CDATA_START = Pattern.compile("(\\<!\\[CDATA\\[).*");
	public static Pattern TAG_CDATA_END = Pattern.compile(".*(]]>)");
	public static Pattern UNDERSPEC_VAR = Pattern.compile("(\\{.*?\\})");

	/** Colours associated with each pattern */
	static {
		patternColors = new LinkedHashMap<Pattern, Color>();
		patternColors.put(TAG_CDATA_START, new Color(128, 128, 128));
		patternColors.put(TAG_CDATA_END, new Color(128, 128, 128));
		patternColors.put(TAG_PATTERN, new Color(63, 127, 127));
		patternColors.put(TAG_ATTRIBUTE_PATTERN, new Color(127, 0, 127));
		patternColors.put(TAG_END_PATTERN, new Color(63, 127, 127));
		patternColors.put(TAG_ATTRIBUTE_VALUE, new Color(42, 0, 255));
		patternColors.put(TAG_COMMENT, new Color(63, 95, 191));
		patternColors.put(UNDERSPEC_VAR, new Color(142, 150, 255));
	}

	public XMLView(Element element) {

		super(element);

		// Set tabsize to 4 (instead of the default 8)
		getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
	}

	@Override
	protected int drawUnselectedText(Graphics graphics, int x, int y, int p0, int p1)
			throws BadLocationException {

		Document doc = getDocument();
		String text = doc.getText(p0, p1 - p0);

		Segment segment = getLineBuffer();

		List<ColouredText> segments = getColouredSegments(text);
		segments = removeOverlaps(segments);

		int caret = 0;

		// Colour the parts
		for (ColouredText entry : segments) {

			if (caret < entry.start) {
				graphics.setColor(Color.black);
				doc.getText(p0 + caret, entry.start - caret, segment);
				x = Utilities.drawTabbedText(segment, x, y, graphics, this, caret);
			}

			graphics.setColor(entry.colour);
			caret = entry.end;
			doc.getText(p0 + entry.start, caret - entry.start, segment);
			x = Utilities.drawTabbedText(segment, x, y, graphics, this, entry.start);
		}

		// Paint possible remaining text black
		if (caret < text.length()) {
			graphics.setColor(Color.black);
			doc.getText(p0 + caret, text.length() - caret, segment);
			x = Utilities.drawTabbedText(segment, x, y, graphics, this, caret);
		}

		return x;
	}

	/**
	 * Returns a list of coloured segments from the text. The method looks for all
	 * patterns and creates a coloured segment for each occurrence.
	 * 
	 * @param text the text to parse
	 * @return the resulting list of coloured segments
	 */
	private List<ColouredText> getColouredSegments(String text) {

		List<ColouredText> segments = new ArrayList<ColouredText>();

		// Match all regexes on this snippet, store positions
		for (Map.Entry<Pattern, Color> entry : patternColors.entrySet()) {
			Pattern pattern = entry.getKey();
			Color colour = entry.getValue();
			Matcher m = pattern.matcher(text);
			while (m.find()) {
				segments.add(new ColouredText(m.start(1), m.end(), colour));
			}
		}
		Collections.sort(segments);

		return segments;
	}

	/**
	 * Removes all overlaps from the coloured segments, and returns the result.
	 * 
	 * @param segments the initial segments
	 * @return the segments without overlaps
	 */
	private List<ColouredText> removeOverlaps(List<ColouredText> segments) {

		List<ColouredText> segments2 = new ArrayList<ColouredText>(segments);

		Iterator<ColouredText> iterator = segments.iterator();
		ColouredText current = (iterator.hasNext()) ? iterator.next() : null;
		while (iterator.hasNext()) {
			ColouredText next = iterator.next();
			if (current.start == next.start) {
				ColouredText toRemove = (current.end < next.end) ? current : next;
				segments2.remove(toRemove);
			}
			else if (next.start < current.end) {
				current.end = next.start;
				if (next.end < current.end) {
					segments2.add(
							new ColouredText(next.end, current.end, current.colour));
				}
			}
			current = next;
		}
		Collections.sort(segments2);
		return segments2;
	}

	/**
	 * Representation of a segment with a start index, an end index, and an
	 * associated colour.
	 */
	final class ColouredText implements Comparable<ColouredText> {

		int start;
		int end;
		Color colour;

		public ColouredText(int start, int end, Color colour) {
			this.start = start;
			this.end = end;
			this.colour = colour;
		}

		@Override
		public int compareTo(ColouredText o) {
			return Integer.compare(start, o.start);
		}

	}

}