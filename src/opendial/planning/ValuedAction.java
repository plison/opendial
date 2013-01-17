package opendial.planning;

import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;

/**
 * A couple made of an assignment and its corresponding utility. They can
 * be sorted according to their utility.
 *
 */
public final class ValuedAction implements TrajectoryPoint, Comparable<ValuedAction>  {
	
	Assignment actions;
	double reward;
	
	public ValuedAction (Assignment assign, double reward) {
		this.actions = assign;
		this.reward = reward;
	}
	
	public Assignment getAssignment() {
		return actions;
	}
	
	public double getReward() {
		return reward;
	}


	public ProbDistribution getDistribution() {
		SimpleTable actionTable = new SimpleTable();
		actionTable.addRow(actions, 1.0);
		return actionTable;
	}


	public String prettyPrint() {
		String rewardStr = "" + Math.round(reward*1000.0)/1000.0;
		String s = "action: " + actions + " (R=" + rewardStr+")";
		return s;
	}
	
	@Override
	public String toString() {
		return prettyPrint();
	}
	
	@Override
	public int compareTo(ValuedAction o) {
		return (int)((reward - o.getReward())*1000);
		
	}

	public void removeSpecifiers() {
		actions = actions.removeSpecifiers();
	}

}