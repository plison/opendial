package opendial.gui.statemonitor.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;

import opendial.arch.DialException;
import opendial.arch.Settings;
import opendial.bn.distribs.ProbDistribution;
import opendial.gui.statemonitor.DialogueStatePopup;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
import opendial.state.DialogueState;
import opendial.utils.StringUtils;

/**
 * Representation of an inference action triggered by the user via the popup
 * menu.  An inference action is defined via a set of query variables, plus some
 * optional evidence.
 * 
 * TODO: allow use of utility distribution as well
 *
 */
public final class MarginalDistributionAction extends AbstractAction {

	/**
	 * 
	 */
	private final DialogueStatePopup graphViewerPopupMenu;
	Set<String> queryVariables;
	
	public MarginalDistributionAction(DialogueStatePopup graphViewerPopupMenu, String name, Set<String> queryVariables) {
		super(name);
		this.graphViewerPopupMenu = graphViewerPopupMenu;
		this.queryVariables = queryVariables;
	}
	/**
	 *
	 * @param arg0
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
	try {
			DialogueState state = this.graphViewerPopupMenu.getViewer().getDialogueState();
			ProbDistribution distrib = state.getContent(queryVariables, true);
			String str = StringUtils.getHtmlRendering(distrib.prettyPrint().replace(", ", "\n").replace("\n", "\n<br>"));
			this.graphViewerPopupMenu.getViewer().getStateMonitorTab().writeToLogArea(
					"<html><font face=\"helvetica\">"+ str + "</font></html>");
			for (String queryVariable: new HashSet<String>(queryVariables)) {
			this.graphViewerPopupMenu.getViewer().getPickedVertexState().pick(queryVariable, false);
			}

		} catch (DialException e) {
			e.printStackTrace();
			DialogueStatePopup.log.debug("problem performing the inference for P(" + queryVariables +")" +
					" aborting action");
		}
	}
	
}