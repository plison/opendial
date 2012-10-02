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

package oblig2.state;

import oblig2.util.Logger;

/**
 * Representation of the current robot position
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RobotPosition {

	// logger
	public static Logger log = new Logger("RobotPosition", Logger.Level.NORMAL);

	// possible orientations for the robot
	public static enum Orientation {NORTH, SOUTH, WEST, EAST }

	// x coordinates
	int xPos;
	
	// y coordinates
	int yPos;
	
	// orientation
	Orientation orientation;

	
	/**
	 * Creates a new robot position
	 * 
	 * @param xPos x coordinates
	 * @param yPos y coordinates
	 * @param orientation orientation
	 */
	public RobotPosition(int xPos, int yPos, Orientation orientation) {
		this.xPos = xPos;
		this.yPos = yPos;
		this.orientation = orientation;
	}

	/**
	 * Modifies the position of the robot
	 * 
	 * @param xPos x coordinates
	 * @param yPos y coordinates
	 */
	public void setPosition(int xPos, int yPos) {
		this.xPos = xPos;
		this.yPos = yPos;
	}

	
	/**
	 * Modifies the orientation of the robot
	 * 
	 * @param orientation the new orientation
	 */
	public void setOrientation (Orientation orientation) {
		this.orientation = orientation;
	}

	/**
	 * Returns the X coordinate
	 * 
	 * @return x coordinate
	 */
	public int getX() {
		return xPos;
	}

	/**
	 * Returns the Y coordinate
	 * 
	 * @return y coordinate
	 */
	public int getY() {
		return yPos;
	}

	/**
	 * Returns the robot orientation
	 * 
	 * @return the orientation
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * Returns a string representation of the robot position
	 *
	 * @return the string
	 */
	public String toString() {
		String str = "(" + xPos + ", " + yPos + ") ";
		switch (orientation) {
		case NORTH: return str + "facing north";
		case SOUTH: return str + "facing south";
		case WEST: return str + "facing west";
		case EAST: return str + "facing east";
		default: return str;
		}
	}

	/**
	 * Returns the hashcode for this position
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return toString().hashCode();
	}



}
