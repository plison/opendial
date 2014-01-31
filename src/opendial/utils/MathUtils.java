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

package opendial.utils;


import opendial.arch.Logger;


/**
 * Math utilities.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class MathUtils {

	// logger
	public static Logger log = new Logger("MathUtils", Logger.Level.DEBUG);

	
	/**
	 * Returns the log-gamma value using Lanczos approximation formula
	 * 
	 * Reference: http://introcs.cs.princeton.edu/java/91float/Gamma.java
	 * 
	 * @param x the point
	 * @return  the log-gamma value for the point
	 */
	static double logGamma(double x) {
	      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
	      double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
	                       + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
	                       +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
	      return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	   }
	  
	
	/**
	 * Returns the value of the gamma function: 
	 * 	Gamma(x) = integral( t^(x-1) e^(-t), t = 0 .. infinity)
	 * 
	 * @param x the point
	 * @return the gamma value fo the point
	 */
	public  static double gamma(double x) { 
		   return Math.exp(logGamma(x)); 
		 }
	

	/**
	 * Returns the volume of an N-ball of a certain radius.
	 * 
	 * @param radius the radius.
	 * @param dimension the number of dimensions to consider
	 * @return the resulting volume
	 */
	public static double getVolume(double radius, int dimension) {
		double numerator = Math.pow(Math.PI,dimension/2.0);
		double denum = gamma((dimension/2.0) + 1);
		double radius2 = Math.pow(radius, dimension);
		return radius2*numerator/denum;
	}
}

