package opendial.gui.statemonitor.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;

import opendial.arch.ConfigurationSettings;
import opendial.arch.DialogueState;
import opendial.bn.distribs.ProbDistribution;
import opendial.gui.statemonitor.GraphViewerPopupMenu;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.queries.ProbQuery;
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
	private final GraphViewerPopupMenu graphViewerPopupMenu;
	Set<String> queryVariables;
	
	public MarginalDistributionAction(GraphViewerPopupMenu graphViewerPopupMenu, String name, Set<String> queryVariables) {
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
			ConfigurationSettings settings = ConfigurationSettings.getInstance();
			InferenceAlgorithm algorithm = settings.getInferenceAlgorithm().newInstance();
			DialogueState state = this.graphViewerPopupMenu.getViewer().getDialogueState();
			ProbDistribution distrib = algorithm.queryProb(new ProbQuery(state, queryVariables));
			String str = StringUtils.getHtmlRendering(distrib.prettyPrint().replace("\n", "\n<br>"));
			this.graphViewerPopupMenu.getViewer().getStateMonitorTab().writeToLogArea("<html><font face=\"helvetica\">"+ str + "</font></html>");
			for (String queryVariable: new HashSet<String>(queryVariables)) {
			this.graphViewerPopupMenu.getViewer().getPickedVertexState().pick(queryVariable, false);
			}

		} catch (Exception e) {
			e.printStackTrace();
			GraphViewerPopupMenu.log.debug("problem performing the inference for P(" + queryVariables +")" +
					" aborting action");
		}
	}
	
}