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

package opendial.bn.distribs.continuous.functions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opendial.arch.Logger;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.VectorVal;

public class DirichletDensityFunction implements MultivariateDensityFunction {

	// logger
	public static Logger log = new Logger("DirichletDensityFunction",
			Logger.Level.DEBUG);

	double[] alphas;

	double C;


	public DirichletDensityFunction(double[] alphas) {
		this.alphas = alphas;
		if (alphas.length < 2) {
			log.warning("must have at least 2 alphas");
		}
		for (int i = 0 ; i < alphas.length ; i++) {
			if (alphas[i] <= 0) {
				log.warning("alphas of the Dirichlet distribution are not well formed");
			}
		}
		C = calculateC();
	}

	@Override
	public double getDensity(double[] x) {
		if (x.length == alphas.length) {

			double sum = 0;
			for (int i = 0; i < x.length ; i++) {
				if (x[i] <0 || x[i] > 1) {
					log.warning(new VectorVal(x) + " does not satisfy the constraints >= 0 and <= 1");
				}
				sum += x[i];
			}
			if (sum < 0.98 || sum > 1.02) {
				log.warning(new VectorVal(x) + " does not sum to 1.0");
			}

			double result = C;
			for (int i = 0; i < x.length ; i++) {
				result *= Math.pow(x[i], alphas[i]-1);
			}
			return result;
		}
		log.warning("incompatible sizes: " + x.length + "!=" + alphas.length);
		return 0.0;
	}


	public int getDimensionality() {
		return alphas.length;
	}

	@Override
	public double[] sample() {

		double sum = 0;
		double[] sample = new double[alphas.length];
		for (int i = 0 ; i < alphas.length ; i++) {
			sample[i] = sampleFromGamma(alphas[i]);
			sum += sample[i];
		}
		for (int i = 0 ; i < alphas.length ; i++) {
			sample[i] = sample[i] / sum;
		}
		return sample;		
	}

	@Override
	public List<double[]> getDiscreteValueArrays(int nbBuckets) {
		List<double[]> values = new ArrayList<double[]>();
		for (int i = 0 ; i < nbBuckets ; i++) {
			values.add(sample());
		}
		return values;
	}

	@Override
	public MultivariateDensityFunction copy() {
		return new DirichletDensityFunction(alphas);
	}

	@Override
	public String prettyPrint() {
		return "Dirichlet(" + Arrays.asList(alphas) + ")";
	}


	public double calculateC() {
		double alphaSum = 0;
		double denominator = 1;
		for (int i = 0 ; i < alphas.length ;i++) {
			alphaSum += alphas[i];
			denominator *= gamma(alphas[i]);
		}
		double numerator = gamma(alphaSum);
		if (denominator != 0.0) {
		return numerator / denominator;
		}
		else {
			return Double.MAX_VALUE;
		}
	}
	

	// Lanczos approximation formula
	  static double logGamma(double x) {
	      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
	      double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
	                       + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
	                       +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
	      return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	   }
	  
	   static double gamma(double x) { 
		   return Math.exp(logGamma(x)); 
		 }




	public synchronized double sampleFromGamma(double alpha) {
		UniformDensityFunction uniform = new UniformDensityFunction(0.0,1.0);
		double gamma=0;
		if (alpha <= 0) {
			throw new IllegalArgumentException ("alpha and beta must be strictly positive.");
		}
		if (alpha < 1) {
			double b,p;
			boolean flag=false;
			b=1+alpha*Math.exp(-1);
			while(!flag) {
				p=b*uniform.sample();
				if (p>1) {
					gamma=-Math.log((b-p)/alpha);
					if (uniform.sample()<=Math.pow(gamma,alpha-1)) flag=true;
				}
				else {
					gamma=Math.pow(p,1/alpha);
					if (uniform.sample()<=Math.exp(-gamma)) flag=true;
				}
			}
		}
		else if (alpha == 1) {
			gamma = -Math.log (uniform.sample());
		} else {
			double y = -Math.log (uniform.sample());
			while (uniform.sample() > Math.pow (y * Math.exp (1 - y), alpha - 1))
				y = -Math.log (uniform.sample());
			gamma = alpha * y;
		}
		return gamma;
	}

	@Override
	public double[] getMean() {
		double[] mean = new double[alphas.length];
		for (int i = 0 ; i < alphas.length ; i++) {
			mean[i] = alphas[i] / getAlphaSum();
		}
		return mean;
	}
	
	
	private double getAlphaSum() {
		double sum = 0;
		for (int j = 0 ; j < alphas.length ; j++) {
			sum += alphas[j];
		}
		return sum;
	}

	@Override
	public double[] getVariance() {
		double[] variance = new double[alphas.length];
		double denominator = Math.pow(getAlphaSum(), 2) * (getAlphaSum() + 1);
		for (int j = 0 ; j < alphas.length ; j++) {
			double numerator = alphas[j]*(getAlphaSum() - alphas[j]);
			variance[j] = numerator / denominator;
		}
		return variance;
	}
}

