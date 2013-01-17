package opendial.gui.statemonitor.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import opendial.arch.ConfigurationSettings;
import opendial.bn.nodes.ChanceNode;
import opendial.gui.statemonitor.DialogueStatePopup;
import opendial.gui.statemonitor.options.EvidenceMarkPanel;

@SuppressWarnings("serial")
public final class EvidenceMarkAction extends AbstractAction {

	/**
	 * 
	 */
	private final DialogueStatePopup graphViewerPopupMenu;
	String evidenceVariable;
	
	public EvidenceMarkAction(DialogueStatePopup graphViewerPopupMenu, String name, String evidenceVariable) {
		super(name);
		this.graphViewerPopupMenu = graphViewerPopupMenu;
		this.evidenceVariable = evidenceVariable;
	}
	/**
	 *
	 * @param arg0
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			ConfigurationSettings settings = ConfigurationSettings.getInstance();
			JFrame frame = this.graphViewerPopupMenu.getViewer().getStateMonitorTab().getMainFrame();
			ChanceNode node = this.graphViewerPopupMenu.getViewer().getDialogueState().getNetwork().getChanceNode(evidenceVariable);
			new EvidenceMarkPanel(frame, node, this.graphViewerPopupMenu.getViewer().getDialogueState());
			this.graphViewerPopupMenu.getViewer().getPickedVertexState().pick(evidenceVariable, false);
			
		} catch (Exception e) {
			DialogueStatePopup.log.debug("problem performing the inference for P(" + evidenceVariable +") " +
					"aborting action: " + e.toString());
		}	
	}
}