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
 * Representation of a visual object in the scene
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class VisualObject {

	// logger
	public static Logger log = new Logger("ObjectDescription", Logger.Level.NORMAL);

	// Possible shapes and colors
	public static enum Shape {CUBE, CYLINDER }
	public static enum Colour {RED, BLUE, GREEN }

	// x coordinate for the object
	int xPos;
	
	// y coordinate for the object
	int yPos;
	
	// object shape
	Shape shape;
	
	// object colour
	Colour colour;

	/**
	 * Creates a new visual object with the given coordinates and properties
	 * 
	 * @param xPos x coordinate
	 * @param yPos y coordinate
	 * @param shape object shape
	 * @param colour object colour
	 */
	public VisualObject (int xPos, int yPos, Shape shape, Colour colour) {
		this.xPos = xPos;
		this.yPos = yPos;
		this.shape = shape;
		this.colour = colour;
	}

	/**
	 * Modifies the object position
	 * 
	 * @param xPos new X coordinate
	 * @param yPos new Y coordinate
	 */
	public void setPosition(int xPos, int yPos) {
		this.xPos = xPos;
		this.yPos = yPos;
	}

	/**
	 * Returns the X coordinate
	 * 
	 * @return X coordinate
	 */
	public int getX() {
		return xPos;
	}

	/**
	 * Returns the Y coordinate
	 * 
	 * @return Y coordinate
	 */
	public int getY() {
		return yPos;
	}

	/**
	 * Returns the object shape
	 * 
	 * @return object shape
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * Returns the object colour
	 * 
	 * @return object colour
	 */
	public Colour getColour() {
		return colour;
	}

	/**
	 * Returns a string representation of the object
	 *
	 * @return the string
	 */
	public String toString() {

		String str = "";
		switch (colour) {
		case RED: str = "red "; break;
		case GREEN: str = "green "; break;
		case BLUE: str = "blue "; break;
		}
		switch (shape) {
		case CUBE: str = "cube "; break;
		case CYLINDER: str = "cylinder "; break;
		}

		return str + "at (" + xPos + ", " + yPos + ") ";
	}


}
