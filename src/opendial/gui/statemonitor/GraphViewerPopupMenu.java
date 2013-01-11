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

package opendial.gui.statemonitor;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;

import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.picking.PickedState;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.nodes.BNode;
import opendial.bn.nodes.ChanceNode;
import opendial.gui.statemonitor.actions.DistributionChartAction;
import opendial.gui.statemonitor.actions.EvidenceMarkAction;
import opendial.gui.statemonitor.actions.MarginalDistributionAction;
import opendial.gui.statemonitor.actions.NewNodeAction;
import opendial.gui.statemonitor.options.NodeEditPanel;

/**
 * Mouse plugin to handle the right-click popup after selecting nodes
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class GraphViewerPopupMenu extends AbstractPopupGraphMousePlugin 
implements MouseListener {

	// logger
	public static Logger log = new Logger("GraphViewerPopupMenu", Logger.Level.DEBUG);
	
	private GraphViewer viewer;
	
    public GraphViewerPopupMenu(GraphViewer viewer) {
        this(viewer, MouseEvent.BUTTON3_MASK);
    }
    public GraphViewerPopupMenu(GraphViewer viewer, int modifiers) {
        super(modifiers);
    	this.setViewer(viewer);
    }

    /**
     * Creates the popup with relevant action items
     *
     * @param e the mouse event
     */ 
    protected void handlePopup(MouseEvent e) {
    	super.mouseClicked(e);			
		Set<String> pickedVertices = getViewer().getPickedVertexState().getPicked();
		JPopupMenu popup = new JPopupMenu();
		if (suitedForInference(pickedVertices)) {
			popup.add(new MarginalDistributionAction(this, "Display marginal distribution", 
					pickedVertices));
		}
		if (suitedForDistribDisplay(pickedVertices)) {
			popup.add(new DistributionChartAction(this, "Show distribution chart", 
					pickedVertices.iterator().next()));
		}
		if (suitedForEvidence(pickedVertices)) {
			popup.add(new EvidenceMarkAction(this, "Mark as evidence node", 
					pickedVertices.iterator().next()));
		}
		if (suitedForAddition(pickedVertices)) {
			popup.add(new NewNodeAction(this, "Add new node"));
		}
		// other action: draw outgoing dependency
		
		if (popup.getComponentCount() == 0 && !pickedVertices.isEmpty()) {
			popup.add(new JLabel("  No action available for the selected node(s)  "));
		}
		if (popup.getComponentCount() > 0) {
			popup.show(getViewer(), e.getX(), e.getY());
		}
    }
   
	/**
     * Determines whether the inference action is appropriate in this setting
     * (selection must consist of chance nodes, of size between 1 and 3.
     * 
     * @param graphIds the identifier for the vertices
     * @return whether the inference action is appropriate
     */
    private boolean suitedForInference(Set<String> graphIds) {
    	if (graphIds.size() == 0 || graphIds.size() > 4) {
    		return false;
    	}
    	for (String graphId : graphIds) {
    		BNode node = getViewer().getBNode(graphId);
    		if (!(node instanceof ChanceNode)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private boolean suitedForDistribDisplay(Set<String> graphIds) {
    	if (graphIds.size() != 1) {
    		return false;
    	}
    	return (getViewer().getBNode(graphIds.iterator().next()) instanceof ChanceNode);
    }
    
    
    /**
	 * 
	 * @param pickedVertices
	 * @return
	 */
	private boolean suitedForEvidence(Set<String> graphIds) {
		return (graphIds.size() == 1 && 
				(getViewer().getBNode(graphIds.iterator().next())) instanceof ChanceNode);
	}
	
	private boolean suitedForAddition(Set<String> graphIds) {
		return (graphIds.isEmpty());
	}
    

	public GraphViewer getViewer() {
		return viewer;
	}
	public void setViewer(GraphViewer viewer) {
		this.viewer = viewer;
	}

	
}
