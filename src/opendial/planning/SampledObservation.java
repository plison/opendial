package opendial.planning;

import org.jfree.util.Log;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.continuous.functions.GaussianDensityFunction;
import opendial.bn.distribs.discrete.SimpleTable;

/**
 * A couple made of an assignment and its corresponding utility. They can
 * be sorted according to their utility.
 *
 */
public final class SampledObservation implements TrajectoryPoint  {
	
	// logger
	public static Logger log = new Logger("SampledObservation", Logger.Level.DEBUG);

	public static double UNCERTAINTY_VARIANCE = 0.005;

	Assignment observation;
	SimpleTable distrib;


	public SampledObservation(Assignment sample) {
		this.observation = sample;
		
		distrib = wrapUncertainty();
	}
	
	
	private SimpleTable wrapUncertainty() {
		
		GaussianDensityFunction gaussian = new GaussianDensityFunction(
				Settings.observationUncertainty, UNCERTAINTY_VARIANCE);
		double sampledUncertainty = gaussian.sample();
		sampledUncertainty = (sampledUncertainty > 1.0)? 1.0 : sampledUncertainty;
		sampledUncertainty = (sampledUncertainty < 0.02)? 0.05 : sampledUncertainty;
		SimpleTable table = new SimpleTable();
		table.addRow(observation, sampledUncertainty);
		return table;
	}
	
	
	public boolean isNone() {
		return observation.isDefault() ;
	}
	
	public ProbDistribution getDistribution() {
		return distrib;
	}
	
	
	public String prettyPrint() {
		String observationStr = "" + Math.round(distrib.getProb(observation)*1000.0)/1000.0;
		return "observation: " +  observation +  " (P=" + observationStr + ")";
	}
	
	
	@Override
	public String toString() {
		return prettyPrint();
	}

}