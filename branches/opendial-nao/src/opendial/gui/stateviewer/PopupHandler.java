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

package opendial.gui.stateviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import opendial.arch.Logger;
import opendial.bn.distribs.ProbDistribution;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

/**
 * Mouse plugin to handle the right-click popup after selecting nodes
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PopupHandler extends AbstractPopupGraphMousePlugin 
implements MouseListener, ActionListener {

	// logger
	public static Logger log = new Logger("GraphViewerPopupMenu", Logger.Level.DEBUG);

	public static final String MARGINAL = "Calculate marginal distribution";
	public static final String DISTRIB = "Show distribution chart";
	public static final String UTILITY = "Calculate utility";


	private StateViewer viewer;

	/**
	 * Constructs the popup handler for the state viewer.
	 * 
	 * @param viewer the state viewer component.
	 */
	public PopupHandler(StateViewer viewer) {
		super(InputEvent.BUTTON3_MASK);
		this.viewer = viewer;
	}


	/**
	 * Creates the popup with relevant action items
	 *
	 * @param e the mouse event
	 */ 
	@Override
	protected void handlePopup(MouseEvent e) {
		super.mouseClicked(e);			
		List<String> pickedVertices = new ArrayList<String>(viewer.getPickedVertexState().getPicked());
		
		JPopupMenu popup = new JPopupMenu();
		if (!pickedVertices.isEmpty() && viewer.getState().hasChanceNodes(pickedVertices)) {
			JMenuItem marginalItem = new JMenuItem(MARGINAL);
			marginalItem.addActionListener(this);
			popup.add(marginalItem);
		}
		if (pickedVertices.size() == 1 && viewer.getState().hasChanceNode(pickedVertices.get(0))) {
			JMenuItem distribItem = new JMenuItem(DISTRIB);
			distribItem.addActionListener(this);
			popup.add(distribItem);
	/**		JMenuItem evidenceItem = new JMenuItem(EVIDENCE);
			evidenceItem.addActionListener(this);
			popup.add(evidenceItem); */
		}
		if (pickedVertices.isEmpty()) {
	/**		JMenuItem addNewItem = new JMenuItem(ADD_NEW);
			addNewItem.addActionListener(this);
			popup.add(addNewItem); */
		}
		if (!viewer.getState().getUtilityNodeIds().isEmpty()) {
			JMenuItem utilityItem = new JMenuItem(UTILITY);
			utilityItem.addActionListener(this);
			popup.add(utilityItem);
		}
		
		// other action: draw outgoing dependency

		if (popup.getComponentCount() == 0 && !pickedVertices.isEmpty()) {
			popup.add(new JLabel("  No action available for the selected node(s)  "));
		}
		if (popup.getComponentCount() > 0) {
			popup.show(viewer, e.getX(), e.getY());
		}
	}


	/**
	 * Executes the action corresponding to the button clicked in the popup menu.
	 * 
	 * @param e the action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		DialogueState state = viewer.getState();
		Set<String> pickedVertices = new HashSet<String>();
		for (String vertice : viewer.getPickedVertexState().getPicked()) {
			pickedVertices.add(viewer.getBNode(vertice).getId());
		}
		pickedVertices.removeAll(state.getUtilityNodeIds());

		if (e.getSource() instanceof JMenuItem) {

			if (((JMenuItem)e.getSource()).getText().equals(MARGINAL)) {
				ProbDistribution distrib = state.queryProb(pickedVertices);
				String str = StringUtils.getHtmlRendering(distrib.toString().replace("\n", "\n<br>"));
				viewer.getStateMonitorTab().writeToLogArea(
						"<html><font face=\"helvetica\">"+ str + "</font></html>");
			}

			else if (((JMenuItem)e.getSource()).getText().equals(DISTRIB)) {
				viewer.displayDistrib(pickedVertices);
			}

			else if (((JMenuItem)e.getSource()).getText().equals(UTILITY)) {
				if (pickedVertices.isEmpty()) {
					pickedVertices = state.getActionNodeIds();
				}
				String result = (pickedVertices.isEmpty())? ""+state.queryUtil() 
						: state.queryUtil(pickedVertices).toString();
				String str = StringUtils.getHtmlRendering(result.replace("\n", "\n<br>"));
				viewer.getStateMonitorTab().writeToLogArea(
						"<html><font face=\"helvetica\">"+ str + "</font></html>");
			}

			for (String queryVariable: new HashSet<String>(pickedVertices)) {
				viewer.getPickedVertexState().pick(queryVariable, false);
			}
		}
	}


}
