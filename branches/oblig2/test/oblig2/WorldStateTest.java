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

import static org.junit.Assert.*;

import org.junit.Test;

import oblig2.actions.PhysicalAction;
import oblig2.actions.PhysicalAction.Movement;
import oblig2.state.RobotPosition.Orientation;
import oblig2.state.VisualObject;
import oblig2.state.WorldState;
import oblig2.util.Logger;

/**
 * Some JUnit testing for the WorldState
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class WorldStateTest {

	// logger
	public static Logger log = new Logger("WorldStateTest", Logger.Level.DEBUG);
	
	// AT&T parameters
	static String uuid = "F9A9D13BC9A811E1939C95CDF95052CC";
	static String	appname = "def001";
	static String	grammar = "numbers";
	
	@Test
	public void basicMovements() throws InterruptedException {

		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
		parameters.activateGUI = false;
		
		DialoguePolicy policy = new BasicPolicy();
		
		DialogueSystem system = new DialogueSystem(policy, parameters);
		system.start();
		
		WorldState state = system.getWorldState();
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());		

		state.executeAction(new PhysicalAction(Movement.BACKWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.LEFT));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.WEST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.WEST, state.getRobotPosition().getOrientation());
				
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.BACKWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.LEFT));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());	
	}
	
	
	@Test
	public void pickingUp() throws InterruptedException {

		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
		parameters.activateGUI = false;
		
		DialoguePolicy policy = new BasicPolicy();
		
		DialogueSystem system = new DialogueSystem(policy, parameters);
		system.start();
		
		WorldState state = system.getWorldState();
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());		

		assertEquals(null, state.getCarriedObject());

		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.LEFT));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.LEFT));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.WEST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.WEST, state.getRobotPosition().getOrientation());
		
		boolean pickedUpObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertFalse(pickedUpObject);
		assertEquals(null, state.getCarriedObject());
		
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		pickedUpObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertTrue(pickedUpObject);
		assertNotNull(state.getCarriedObject());
				
	}
	
	
	
	

	@Test
	public void releasing() throws InterruptedException {

		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
		parameters.activateGUI = false;
		
		DialoguePolicy policy = new BasicPolicy();
		
		DialogueSystem system = new DialogueSystem(policy, parameters);
		system.start();
		
		WorldState state = system.getWorldState();
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());		

		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(0, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		assertEquals(0, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.EAST, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.LEFT));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(1, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(3, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
			
		boolean pickedUpObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertTrue(pickedUpObject);
		assertNotNull(state.getCarriedObject());

		state.executeAction(new PhysicalAction(Movement.PUTDOWN));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(3, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		boolean putDownObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY() + 1) {
				putDownObject = true;
			}
		}
		assertTrue(putDownObject);
		assertNull(state.getCarriedObject());
		
		state.executeAction(new PhysicalAction(Movement.PICK));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(3, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(4, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		state.executeAction(new PhysicalAction(Movement.PUTDOWN));
		assertEquals(1, state.getRobotPosition().getX());
		assertEquals(4, state.getRobotPosition().getY());
		assertEquals(Orientation.NORTH, state.getRobotPosition().getOrientation());
		
		pickedUpObject = false;
		putDownObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY() + 1) {
				putDownObject = true;
			}
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertFalse(putDownObject);	
		assertTrue(pickedUpObject);	
		
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		state.executeAction(new PhysicalAction(Movement.FORWARD));
		assertEquals(4, state.getRobotPosition().getX());
		assertEquals(2, state.getRobotPosition().getY());
		assertEquals(Orientation.SOUTH, state.getRobotPosition().getOrientation());
		state.executeAction(new PhysicalAction(Movement.PUTDOWN));

		pickedUpObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertTrue(pickedUpObject);	
		
		state.executeAction(new PhysicalAction(Movement.PICK));
		
		boolean objStillThere = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY() - 1) {
				objStillThere = true;
			}
		}
		assertTrue(objStillThere);	
		
		state.executeAction(new PhysicalAction(Movement.RIGHT));
		state.executeAction(new PhysicalAction(Movement.PUTDOWN));

		putDownObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX()-1 && o.getY() == state.getRobotPosition().getY()) {
				putDownObject = true;
			}
		}
		assertTrue(putDownObject);	
		state.executeAction(new PhysicalAction(Movement.LEFT));
		state.executeAction(new PhysicalAction(Movement.PICK));
		pickedUpObject = false;
		for (VisualObject o: state.getAllObjects()) {
			if (o.getX() == state.getRobotPosition().getX() && o.getY() == state.getRobotPosition().getY()) {
				pickedUpObject = true;
			}
		}
		assertTrue(pickedUpObject);
	}
	
	
	@Test
	public void objectsInSight() throws InterruptedException {
		
		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
			parameters.activateGUI = false;
			
			DialoguePolicy policy = new BasicPolicy();
			
			DialogueSystem system = new DialogueSystem(policy, parameters);
			system.start();
			
			WorldState state = system.getWorldState();
			assertEquals(0, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));

			assertEquals(1, state.getObjectsInSight().size());
			
			state.executeAction(new PhysicalAction(Movement.RIGHT));
			assertEquals(0, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			assertEquals(1, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			assertEquals(1, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.LEFT));
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			state.executeAction(new PhysicalAction(Movement.LEFT));
			assertEquals(1, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			assertEquals(1, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.RIGHT));
			assertEquals(1, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			assertEquals(0, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.RIGHT));
			assertEquals(0, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.FORWARD));
			assertEquals(0, state.getObjectsInSight().size());
			state.executeAction(new PhysicalAction(Movement.RIGHT));
			assertEquals(1, state.getObjectsInSight().size());

	}
}
