package opendial.gui.statemonitor.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import opendial.gui.statemonitor.GraphViewerPopupMenu;
import opendial.gui.statemonitor.options.NodeEditPanel;


public final class NewNodeAction extends AbstractAction {

	private final GraphViewerPopupMenu graphViewerPopupMenu;
	
	public NewNodeAction(GraphViewerPopupMenu graphViewerPopupMenu, String name) {
		super(name);
		this.graphViewerPopupMenu = graphViewerPopupMenu;
	}
	
	/**
	 *
	 * @param arg0
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFrame frame = graphViewerPopupMenu.getViewer().getStateMonitorTab().getMainFrame();
		new NodeEditPanel(frame);
		
	}
	
}
					
	