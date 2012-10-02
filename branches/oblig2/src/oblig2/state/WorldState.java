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

import java.util.HashSet;
import java.util.Set;

import oblig2.ConfigParameters;
import oblig2.actions.PhysicalAction;
import oblig2.util.Logger;
import oblig2.state.RobotPosition.Orientation;

/**
 * Representation of the "world" state, constituted by a robot in a simulated
 * environment, along with a set of visual objects in the scene.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class WorldState {

	// logger
	public static Logger log = new Logger("WorldState", Logger.Level.DEBUG);

	// size of the grid for our simulated world
	int gridSizeX, gridSizeY;

	// robot position
	RobotPosition robotPos;

	// visual objects
	Set<VisualObject> objects;

	// listeners attached to the world
	Set<WorldStateListener> listeners;

	/**
	 * Creates a new world, given some parameters
	 * 
	 * @param parameters the parameters
	 */
	public WorldState(ConfigParameters parameters) {
		listeners = new HashSet<WorldStateListener>();
		objects = new HashSet<VisualObject>();
		robotPos = parameters.initRobotPos;
		for (VisualObject obj: parameters.initObjects) {
			objects.add(obj);
		}
		gridSizeX = parameters.gridSizeX;
		gridSizeY = parameters.gridSizeY;
		log.debug("grid: (" + gridSizeX + ", "+ gridSizeY + ")");
	}

	/**
	 * Attaches a new listener to the world state
	 * 
	 * @param listener the listener
	 */
	public void addListener(WorldStateListener listener) {
		listeners.add(listener);
	}

	/**
	 * Adds a new visual object to the world
	 * 
	 * @param obj the object to add
	 */
	protected void addVisualObject(VisualObject obj) {
		objects.add(obj);
	}


	/**
	 * Returns the robot position
	 * 
	 * @return robot position
	 */
	public RobotPosition getRobotPosition() {
		return robotPos;
	}

	/**
	 * Returns the set of visual objects
	 * 
	 * @return the visual objects
	 */
	public Set<VisualObject> getAllObjects() {
		return new HashSet<VisualObject>(objects);
	}


	public Set<VisualObject> getObjectsInSight() {
		Set<VisualObject> objectsInSight = new HashSet<VisualObject>();
		for (VisualObject o : objects) {
			int incrX = o.getX() - robotPos.getX();
			int incrY = o.getY() - robotPos.getY(); 
			switch (robotPos.getOrientation()) {
			case NORTH: 
				if ((incrX == -1 && incrY == 1) || 
					(incrX == 0 && incrY == 1) || 
					(incrX == 1 && incrY == 1) || 
					(incrX == -1 && incrY == 2) ||
					(incrX == 0 && incrY == 2) || 
					(incrX == 1 && incrY == 2)) {
				objectsInSight.add(o);
			}
			break;
			case WEST: if ((incrX == -1 && incrY == -1) || 
					(incrX == -1 && incrY == 0) || 
					(incrX == -1 && incrY == 1) || 
					(incrX == -2 && incrY == 1) ||
					(incrX == -2 && incrY == 0) || 
					(incrX == -1 && incrY == -1)) {
				objectsInSight.add(o);
			}
			break;
			case SOUTH: if ((incrX == -1 && incrY == -1) ||
					(incrX == 0 && incrY == -1) ||
					(incrX == 1 && incrY == -1) || 
					(incrX == -1 && incrY == -2) ||
					(incrX == 0 && incrY == -2) || 
					(incrX == 1 && incrY == -2)) {
				objectsInSight.add(o);
			}
			break;
			case EAST: if ((incrX == 1 && incrY == -1) ||
					(incrX == 1 && incrY == 0) ||
					(incrX == 1 && incrY == 1) || 
					(incrX == 2 && incrY == 1) ||
					(incrX == 2 && incrY == 0) || 
					(incrX == 1 && incrY == -1)) {
				objectsInSight.add(o);
			}
			break;
			}
		}
		return objectsInSight;
	}


	/**
	 * If the robot is carrying an object, returns it.  Else, returns null.
	 * 
	 * @return an object or null
	 */
	public VisualObject getCarriedObject() {
		for (VisualObject obj : objects) {
			if (obj.getX() == robotPos.getX() && obj.getY() == robotPos.getY()) {
				return obj;
			}
		}
		return null;
	}




	/**
	 * Returns the horizontal size of the grid
	 * 
	 * @return horizontal size
	 */
	public int getGridSizeX() {
		return gridSizeX;
	}

	/**
	 * Returns the vertical size of the grid
	 * 
	 * @return vertical size
	 */
	public int getGridSizeY() {
		return gridSizeY;
	}


	// ==============================================================
	// ACTION EXECUTION METHODS
	// ==============================================================


	/**
	 * Executes the given action in the world
	 * 
	 * @param action physical action to execute
	 */
	public void executeAction (PhysicalAction action) {
		log.debug("movement to execute: "  + action);
		switch (action.getMovement()) {
		case FORWARD: 
			switch (robotPos.getOrientation()) {
			case NORTH: goToRelative (0, 1); break;
			case SOUTH: goToRelative (0, -1); break;
			case WEST: goToRelative (-1, 0); break;
			case EAST: goToRelative(1, 0); break;
			} ; break;
		case BACKWARD: 
			switch (robotPos.getOrientation()) {
			case NORTH: goToRelative (0, -1); break;
			case SOUTH: goToRelative (0, 1); break;
			case WEST: goToRelative (1, 0); break;
			case EAST: goToRelative(-1, 0); break;
			} ; break;	
		case LEFT:
			switch (robotPos.getOrientation()) {
			case NORTH: robotPos.setOrientation(Orientation.WEST); break;
			case SOUTH: robotPos.setOrientation(Orientation.EAST); break;
			case WEST: robotPos.setOrientation(Orientation.SOUTH); break;
			case EAST: robotPos.setOrientation(Orientation.NORTH); break;
			} ; break;	
		case RIGHT:
			switch (robotPos.getOrientation()) {
			case NORTH: robotPos.setOrientation(Orientation.EAST); break;
			case SOUTH: robotPos.setOrientation(Orientation.WEST); break;
			case WEST: robotPos.setOrientation(Orientation.NORTH); break;
			case EAST: robotPos.setOrientation(Orientation.SOUTH); break;
			} ; break;		
		case PICK: pickUpObject(); break ;
		case PUTDOWN :putDownObject(); break;
		}

		for (WorldStateListener listener: listeners) {
			listener.updatedWorldState();
		}
	}


	/**
	 * Move robot by a relative increment on the grid
	 * 
	 * @param incrX increment on the X axis
	 * @param incrY increment on the Y axis
	 */
	private void goToRelative (int incrX, int incrY) {

		int newXPos = robotPos.getX() + incrX;
		int newYPos = robotPos.getY() + incrY;
		if (newXPos >= gridSizeX || newYPos >= gridSizeY || newXPos < 0 || newYPos < 0) {
			log.debug("ignoring command, we have to stay inside the grid");
			return;
		}
		else {
			VisualObject carriedObject = null;
			for (VisualObject obj : objects) {
				if (obj.getX() == newXPos && obj.getY() == newYPos) {
					log.debug("ignoring command, we cannot enter a grid already occupied by an object");
					return;
				}
				if (obj.getX() == robotPos.getX() && obj.getY() == robotPos.getY()) {
					carriedObject = obj;
				}
			}
			log.debug("changing position to (" + newXPos + ", " + newYPos + ")");
			robotPos.setPosition(newXPos, newYPos);
			if (carriedObject != null) {
				carriedObject.setPosition(newXPos, newYPos);
			}
		}
	}


	/**
	 * Picks up the object in front of the robot (if any)
	 */
	private void pickUpObject() {

		int expectedObjXPos;
		int expectedObjYPos;
		switch (robotPos.getOrientation()) {
		case NORTH: expectedObjXPos = robotPos.getX() ; expectedObjYPos = robotPos.getY() + 1; break;
		case SOUTH: expectedObjXPos = robotPos.getX() ; expectedObjYPos = robotPos.getY() - 1; break;
		case WEST: expectedObjXPos = robotPos.getX() -1 ; expectedObjYPos = robotPos.getY() ; break;
		case EAST: expectedObjXPos = robotPos.getX() +1 ; expectedObjYPos = robotPos.getY() ; break;
		default: return ;
		}
		if (getCarriedObject() == null) {	
			boolean found = false;
			for (VisualObject obj : objects) {
				if (obj.getX() == expectedObjXPos && obj.getY() == expectedObjYPos) {
					log.debug("found object to pick up");
					obj.setPosition(robotPos.getX(), robotPos.getY());
					found = true;
				}
			}
			if (!found) {
				log.debug("found no object to pick up, ignoring command");
			}
		}
	}

	/**
	 * Puts down the object in front of the robot (if any)
	 */
	private void putDownObject() {

		int expectedObjXPos;
		int expectedObjYPos;
		switch (robotPos.getOrientation()) {
		case NORTH: expectedObjXPos = robotPos.getX() ; expectedObjYPos = robotPos.getY() + 1; break;
		case SOUTH: expectedObjXPos = robotPos.getX() ; expectedObjYPos = robotPos.getY() - 1; break;
		case WEST: expectedObjXPos = robotPos.getX() -1 ; expectedObjYPos = robotPos.getY() ; break;
		case EAST: expectedObjXPos = robotPos.getX() +1 ; expectedObjYPos = robotPos.getY() ; break;
		default: return ;
		}
		if (expectedObjXPos >= gridSizeX || expectedObjYPos >= gridSizeY || expectedObjXPos < 0 || expectedObjYPos < 0 ) {
			log.debug("cannot put down object there (must be inside the grid), ignoring command");
			return;
		}
		VisualObject objToPutDown = null;
		for (VisualObject obj : objects) {
			if (obj.getX() == expectedObjXPos && obj.getY() == expectedObjYPos) {
				log.debug("object already placed there, cannot put a second object at this place");
				return;
			}
			if (obj.getX() == robotPos.getX() && obj.getY() == robotPos.getY()) {
				objToPutDown = obj;
				log.debug("found object to put down");
			}
		}
		if (objToPutDown != null) {
			objToPutDown.setPosition(expectedObjXPos, expectedObjYPos);
			log.info("object is put down on the floor");
		}
	}
}
