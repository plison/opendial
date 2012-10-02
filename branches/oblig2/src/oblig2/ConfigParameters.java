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

package oblig2;

import java.util.HashSet;
import java.util.Set;

import oblig2.state.VisualObject;
import oblig2.state.VisualObject.Colour;
import oblig2.state.VisualObject.Shape;
import oblig2.state.RobotPosition;
import oblig2.state.RobotPosition.Orientation;


/**
 * Repository of various paramters for the dialogue system
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ConfigParameters {

	/** OBLIGATORY PARAMETERS (must be specified) */
	public String uuid;
	public String appname;
	public String grammar;
	
	/** OPTIONAL PARAMETERS (can be left as default) */
	
	// temporary files in which to save the audio
	public String tempASRSoundFile = "tmp/input.au";
	public String	tempTTSSoundFile = "tmp/output.au";
	
	// test file for AT&T connection
	public String testASRFile = "resources/onetwothreefourfive.au";

	// minimum recording time, in milliseconds
	public int minimumRecordingTime = 1000;
	
	// grid size
	public int gridSizeX = 5, gridSizeY = 5;
	
	// number of elements in N-Best list
	public int nbest = 1;
	
	// initial robot position
	public RobotPosition initRobotPos;
	
	// set of visual objects
	public Set<VisualObject> initObjects;
	
	/**
	 * Creates a new set of configuration parameters
	 * 
	 * @param uuid UUID for AT&T
	 * @param appname application name for AT&T
	 * @param grammar grammar for AT&T
	 */
	public ConfigParameters(String uuid, String appname, String grammar) {
		this.uuid = uuid;
		this.appname = appname;
		this.grammar = grammar;
		
		initObjects = new HashSet<VisualObject>();
		
		initialiseSimulation();
	}
	
	/**
	 * Ugly, we should ideally read this from a config file
	 */
	public void initialiseSimulation() {
		initRobotPos = new RobotPosition(0,0, Orientation.NORTH);
		VisualObject object1 = new VisualObject(1, 3, Shape.CUBE, Colour.BLUE);
		VisualObject object2 = new VisualObject(4, 1, Shape.CYLINDER, Colour.GREEN);
		initObjects.add(object1);
		initObjects.add(object2);
	}
	
		
}
