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

package opendial.gui.stateviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;

import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.picking.PickedState;
import opendial.arch.Logger;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

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
/**	public static final String EVIDENCE = "Mark as evidence node";
	public static final String ADD_NEW = "Add new node"; */
	public static final String UTILITY = "Calculate utility";


	private StateViewer viewer;

	public PopupHandler(StateViewer viewer) {
		super(MouseEvent.BUTTON3_MASK);
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


	@Override
	public void actionPerformed(ActionEvent e) {

		DialogueState state = viewer.getState();
		Set<String> pickedVertices = viewer.getPickedVertexState().getPicked();

		if (e.getSource() instanceof JMenuItem) {

			if (((JMenuItem)e.getSource()).getText().equals(MARGINAL)) {
				ProbDistribution distrib = state.queryProb(pickedVertices);
				String str = StringUtils.getHtmlRendering(distrib.toString().replace(", ", "\n").replace("\n", "\n<br>"));
				viewer.getStateMonitorTab().writeToLogArea(
						"<html><font face=\"helvetica\">"+ str + "</font></html>");
			}

			else if (((JMenuItem)e.getSource()).getText().equals(DISTRIB)) {
				viewer.showDistribution(pickedVertices);
			}

	/**		else if (((JMenuItem)e.getSource()).getText().equals(EVIDENCE)) {
				String evidenceVariable = pickedVertices.iterator().next();
				JFrame frame = viewer.getStateMonitorTab().getMainFrame();
				ChanceNode node = viewer.getState().getChanceNode(evidenceVariable);
				new EvidenceMarkPanel(frame, node, state);
			}
			else if (((JMenuItem)e.getSource()).getText().equals(ADD_NEW)) {
				JFrame frame = viewer.getStateMonitorTab().getMainFrame();
				new NodeEditPanel(frame);
			} */
			else if (((JMenuItem)e.getSource()).getText().equals(UTILITY)) {
				UtilityTable distrib = state.queryUtil(pickedVertices);
				String str = StringUtils.getHtmlRendering(distrib.toString().replace(", ", "\n").replace("\n", "\n<br>"));
				viewer.getStateMonitorTab().writeToLogArea(
						"<html><font face=\"helvetica\">"+ str + "</font></html>");
			}

			for (String queryVariable: new HashSet<String>(pickedVertices)) {
				viewer.getPickedVertexState().pick(queryVariable, false);
			}
		}
	}


}
