// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains;

import java.util.logging.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.datastructs.Assignment;
import opendial.modules.DialogueRecorder;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class FlightBookingTest {

	final static Logger log = Logger.getLogger("OpenDial");

	Domain domain =
			XMLDomainReader.extractDomain("test/domains/example-flightbooking.xml");

	@Test
	public void dialogue1() throws InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.startSystem();
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("your destination?"));
		assertTrue(system.getModule(DialogueRecorder.class).getRecord()
				.contains("<interaction><systemTurn><variable id=\"u_m\">"
						+ "<value>Welcome to our Norwegian flight-booking system!</value></variable></systemTurn>"
						+ "<systemTurn><variable id=\"u_m\"><value>What is your destination?</value></variable></systemTurn>"));
		Map<String, Double> u_u = new HashMap<String, Double>();
		u_u.put("to Bergen", 0.4);
		u_u.put("to Bethleem", 0.2);
		system.addUserInput(u_u);
		assertEquals(0.833,
				system.getContent("a_u").getProb("[Inform(Airport,Bergen)]"), 0.01);
		assertEquals(3, system.getContent("a_u").getValues().size(), 0.01);
		assertEquals(0.055,
				system.getState().queryProb("a_u", false).getProb("[Other]"), 0.01);
		assertEquals("Confirm(Destination,Bergen)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("yes exactly", 0.8);
		system.addUserInput(u_u);
		assertEquals(0.98, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("Destination").getProb("Bergen"), 0.01);
		assertEquals("Ground(Destination,Bergen)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("your departure?"));
		u_u.clear();
		u_u.put("to Stockholm", 0.8);
		system.addUserInput(u_u);
		assertEquals(0.8, system.getContent("a_u").getProb("[Other]"), 0.01);
		assertEquals("Ground(Destination,Bergen)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals(1,
				system.getContent("Destination").toDiscrete().getValues().size());
		assertEquals(1.0, system.getContent("Destination").getProb("Bergen"), 0.01);
		assertFalse(system.getState().hasChanceNode("Departure"));
		assertEquals("AskRepeat", system.getContent("a_m").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("you repeat?"));
		u_u.clear();
		u_u.put("to Sandefjord then", 0.6);
		system.addUserInput(u_u);
		assertEquals(0.149, system.getContent("a_u").getProb("None"), 0.05);
		assertEquals(0.88, system.getContent("Departure").getProb("Sandefjord"),
				0.05);
		assertEquals("Confirm(Departure,Sandefjord)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("that correct?"));
		u_u.clear();
		u_u.put("no to Trondheim sorry", 0.08);
		system.addUserInput(u_u);
		assertEquals(0.51, system.getContent("a_u")
				.getProb("[Inform(Airport,Trondheim),Disconfirm]"), 0.01);
		assertEquals(0.51, system.getContent("Departure").getProb("Trondheim"),
				0.05);
		assertEquals("AskRepeat",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(
				system.getContent("u_m").getBest().toString().contains("repeat?"));
		u_u.clear();
		u_u.put("to Trondheim", 0.3);
		u_u.put("Sandefjord", 0.1);
		system.addUserInput(u_u);
		assertEquals(0.667,
				system.getContent("a_u").getProb("[Inform(Airport,Trondheim)]"),
				0.01);
		assertEquals(1.0, system.getContent("Destination").getProb("Bergen"), 0.01);
		assertEquals(0.89, system.getContent("Departure").getProb("Trondheim"),
				0.01);
		assertEquals("Confirm(Departure,Trondheim)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("yes exactly that's it", 0.8);
		system.addUserInput(u_u);
		assertEquals("Ground(Departure,Trondheim)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("which date"));
		u_u.clear();
		u_u.put("that will be on May 26", 0.4);
		u_u.put("this will be on May 24", 0.2);
		system.addUserInput(u_u);
		assertEquals(0.2, system.getContent("a_u").getProb("[Inform(Date,May,24)]"),
				0.01);
		assertEquals(0.4, system.getContent("a_u").getProb("[Inform(Date,May,26)]"),
				0.01);
		assertEquals(1.0, system.getContent("Destination").getProb("Bergen"), 0.01);
		assertEquals(1.0, system.getContent("Departure").getProb("Trondheim"), 0.01);
		assertEquals(0.4, system.getContent("Date").getProb("May 26"), 0.01);
		assertEquals(0.2, system.getContent("Date").getProb("May 24"), 0.01);
		assertEquals("AskRepeat",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertEquals("Ground(Departure,Trondheim)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("May 24", 0.5);
		u_u.put("Mayday four", 0.5);
		system.addUserInput(u_u);
		assertEquals(0.82, system.getContent("a_u").getProb("[Inform(Date,May,24)]"),
				0.05);
		assertEquals(0.176, system.getContent("a_u").getProb("[Inform(Number,4)]"),
				0.01);
		assertEquals(0.02, system.getContent("Date").getProb("May 26"), 0.01);
		assertEquals(0.94, system.getContent("Date").getProb("May 24"), 0.01);
		assertTrue(system.getState().hasChanceNode("a_m"));
		assertEquals("AskRepeat",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Ground(Date,May 24)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("return trip"));
		u_u.clear();
		u_u.put("no thanks", 0.9);
		system.addUserInput(u_u);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("to order tickets?"));
		assertEquals(1.0, system.getContent("ReturnDate").getProb("NoReturn"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("MakeOffer"),
				0.01);
		assertEquals("MakeOffer(179)",
				system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("yes", 0.02);
		system.addUserInput(u_u);
		assertEquals(0.177, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("MakeOffer"),
				0.01);
		assertFalse(system.getState().hasChanceNode("a_m"));
		u_u.clear();
		u_u.put("yes", 0.8);
		system.addUserInput(u_u);
		assertEquals(0.978, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("many tickets"));
		u_u.clear();
		u_u.put("uh I don't know me", 0.6);
		system.addUserInput(u_u);
		assertEquals(0.6, system.getContent("a_u").getProb("[Other]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		assertEquals("Ground(MakeOffer)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("AskRepeat",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("three tickets please", 0.9);
		system.addUserInput(u_u);
		assertEquals(0.9, system.getContent("a_u").getProb("[Inform(Number,3)]"),
				0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		assertEquals("AskRepeat",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Confirm(NbTickets,3)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("no sorry two tickets", 0.4);
		u_u.put("sorry to tickets", 0.3);
		system.addUserInput(u_u);
		assertEquals(3, system.getContent("a_u").getValues().size(), 0.01);
		assertEquals(0.86,
				system.getContent("a_u").getProb("[Disconfirm,Inform(Number,2)]"),
				0.05);
		assertEquals(0.86, system.getContent("NbTickets").getProb(2), 0.05);
		assertEquals(0.125, system.getContent("NbTickets").getProb(3), 0.05);
		assertEquals("Confirm(NbTickets,3)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Confirm(NbTickets,2)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		u_u.clear();
		u_u.put("yes thank you", 0.75);
		u_u.put("yes mind you", 0.15);
		system.addUserInput(u_u);
		assertEquals(2, system.getContent("a_u").getValues().size(), 0.01);
		assertEquals(1.0, system.getContent("a_u").getProb("[Confirm]"), 0.05);
		assertEquals(1.0, system.getContent("NbTickets").getProb(2), 0.05);
		assertEquals(0.0, system.getContent("NbTickets").getProb(3), 0.05);
		assertEquals("Confirm(NbTickets,2)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Ground(NbTickets,2)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertEquals(1.0, system.getContent("current_step").getProb("LastConfirm"),
				0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("Shall I confirm"));
		assertTrue(
				system.getContent("u_m").getBest().toString().contains("358 EUR"));
		u_u.clear();
		u_u.put("err yes", 0.2);
		system.addUserInput(u_u);
		assertEquals(0.726, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("LastConfirm"),
				0.01);
		u_u.clear();
		u_u.put("yes please confirm", 0.5);
		system.addUserInput(u_u);
		assertEquals(0.934, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("Final"), 0.01);
		assertEquals("Book",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("additional tickets?"));
		u_u.clear();
		u_u.put("thanks but no thanks", 0.7);
		system.addUserInput(u_u);
		assertEquals(0.97, system.getContent("a_u").getProb("[Disconfirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("Close"), 0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("welcome back!"));
		assertTrue(system.getModule(DialogueRecorder.class).getRecord()
				.contains("<systemTurn><variable id=\"u_m\">"
						+ "<value>You are ordering 2 one-way tickets from Trondheim to Bergen on May 24 for a total cost of 358 EUR. "
						+ "Shall I confirm your order?</value></variable>"));
		assertEquals(
				Arrays.asList("Date", "Departure", "Destination", "NbTickets",
						"ReturnDate", "TotalCost", "a_m", "a_m-prev", "a_u",
						"current_step", "u_m", "u_u"),
				system.getState().getChanceNodeIds().stream().sorted()
						.collect(Collectors.toList()));
		assertEquals(1,
				system.getContent(Arrays.asList("Date", "Departure", "Destination",
						"NbTickets", "ReturnDate", "TotalCost")).toDiscrete()
				.getValues().size());
		assertEquals(1.0,
				system.getContent(Arrays.asList("Date", "Departure", "Destination",
						"NbTickets", "ReturnDate", "TotalCost"))
				.toDiscrete()
				.getProb(new Assignment(new Assignment("Date", "May 24"),
						new Assignment("Departure", "Trondheim"),
						new Assignment("Destination", "Bergen"),
						new Assignment("NbTickets", 2),
						new Assignment("ReturnDate", "NoReturn"),
						new Assignment("TotalCost", 358))),
				0.01);
	}

	@Test
	public void dialogue2() throws InterruptedException {
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;
		system.startSystem();
		assertTrue(system.getModule(DialogueRecorder.class).getRecord()
				.contains("<interaction><systemTurn><variable id=\"u_m\">"
						+ "<value>Welcome to our Norwegian flight-booking system!</value></variable></systemTurn>"
						+ "<systemTurn><variable id=\"u_m\"><value>What is your destination?</value></variable></systemTurn>"));
		Map<String, Double> u_u = new HashMap<String, Double>();
		u_u.put("err I don't know, where can I go?", 0.8);
		system.addUserInput(u_u);
		assertEquals(0.8, system.getContent("a_u").toDiscrete().getProb("[Other]"),
				0.01);
		assertEquals("AskRepeat", system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("ah ok well I want to go to Tromsø please", 0.8);
		system.addUserInput(u_u);
		assertEquals(0.91, system.getContent("a_u").toDiscrete()
				.getProb("[Inform(Airport,Tromsø)]"), 0.01);
		assertEquals(0.91,
				system.getContent("Destination").toDiscrete().getProb("Tromsø"),
				0.01);
		assertEquals("Confirm(Destination,Tromsø)",
				system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("that's right", 0.6);
		system.addUserInput(u_u);
		assertEquals("Ground(Destination,Tromsø)",
				system.getContent("a_m").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("departure?"));
		u_u.clear();
		u_u.put("I'll be leaving from Moss", 0.1);
		system.addUserInput(u_u);
		assertEquals(0.357, system.getContent("a_u").toDiscrete()
				.getProb("[Inform(Airport,Moss)]"), 0.01);
		assertEquals(1.0,
				system.getContent("Destination").toDiscrete().getProb("Tromsø"),
				0.01);
		assertEquals(0.357,
				system.getContent("Departure").toDiscrete().getProb("Moss"), 0.01);
		assertEquals("AskRepeat", system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("I am leaving from Moss, did you get that right?", 0.2);
		u_u.put("Bodø, did you get that right?", 0.4);
		system.addUserInput(u_u);
		assertEquals(0.72, system.getContent("a_u").toDiscrete()
				.getProb("[Confirm,Inform(Airport,Moss)]"), 0.01);
		assertEquals(0.88,
				system.getContent("Departure").toDiscrete().getProb("Moss"), 0.01);
		assertEquals(0.10,
				system.getContent("Departure").toDiscrete().getProb("Bodø"), 0.01);
		assertEquals("Confirm(Departure,Moss)",
				system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("yes", 0.6);
		system.addUserInput(u_u);
		assertEquals("Ground(Departure,Moss)",
				system.getContent("a_m").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("which date"));
		u_u.clear();
		u_u.put("March 16", 0.7);
		u_u.put("March 60", 0.2);
		system.addUserInput(u_u);
		assertEquals(0.7, system.getContent("a_u").toDiscrete()
				.getProb("[Inform(Date,March,16)]"), 0.01);
		assertEquals(0.2, system.getContent("a_u").toDiscrete().getProb("[Other]"),
				0.01);
		assertEquals("AskRepeat", system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("March 16", 0.05);
		u_u.put("March 60", 0.3);
		system.addUserInput(u_u);
		assertEquals("Confirm(Date,March 16)",
				system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("yes", 0.6);
		system.addUserInput(u_u);
		assertEquals("Ground(Date,March 16)",
				system.getContent("a_m").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("return trip?"));
		u_u.clear();
		u_u.put("err", 0.1);
		system.addUserInput(u_u);
		assertFalse(system.getState().hasChanceNode("a_m"));
		u_u.clear();
		u_u.put("yes", 0.3);
		system.addUserInput(u_u);
		assertEquals("AskRepeat", system.getContent("a_m").getBest().toString());
		u_u.clear();
		u_u.put("yes", 0.5);
		system.addUserInput(u_u);
		assertEquals(1.0, system.getContent("current_step").getProb("ReturnDate"),
				0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("travel back"));
		u_u.clear();
		u_u.put("on the 20th of March", 0.7);
		system.addUserInput(u_u);
		assertEquals("Confirm(ReturnDate,March 20)",
				system.getContent("a_m").getBest().toString());
		assertEquals(0.7,
				system.getContent("ReturnDate").toDiscrete().getProb("March 20"),
				0.01);
		u_u.clear();
		u_u.put("yes", 0.6);
		system.addUserInput(u_u);
		assertTrue(
				system.getContent("u_m").getBest().toString().contains("299 EUR"));
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("to order tickets?"));
		DialogueState copyState1 = system.getState().copy();
		u_u.clear();
		u_u.put("no", 0.7);
		system.addUserInput(u_u);
		assertEquals("Ground(Cancel)",
				system.getContent("a_m").getBest().toString());
		assertEquals("Final",
				system.getContent("current_step").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("additional tickets?"));
		DialogueState copyState2 = system.getState().copy();
		assertEquals("Final",
				copyState2.queryProb("current_step").getBest().toString());
		u_u.clear();
		u_u.put("no", 0.7);
		system.addUserInput(u_u);
		assertEquals("Final",
				copyState2.queryProb("current_step").getBest().toString());
		assertEquals("Ground(Close)", system.getContent("a_m").getBest().toString());
		assertEquals("Close",
				system.getContent("current_step").getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("welcome back"));
		system.getState().removeNodes(system.getState().getChanceNodeIds());
		system.getState().addNetwork(copyState2);
		assertEquals("Final",
				copyState2.queryProb("current_step").getBest().toString());
		u_u.clear();
		u_u.put("yes", 0.7);
		system.addUserInput(u_u);
		assertFalse(system.getState().hasChanceNode("Destination"));
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("destination?"));
		assertEquals(
				Arrays.asList("Destination^p", "a_m-prev", "current_step", "u_m",
						"u_u"),
				system.getState().getChanceNodeIds().stream().sorted()
						.collect(Collectors.toList()));
		system.addUserInput("Oslo");
		assertEquals("Ground(Destination,Oslo)",
				system.getContent("a_m").getBest().toString());
		system.getState().removeNodes(system.getState().getChanceNodeIds());
		system.getState().addNetwork(copyState1);
		u_u.clear();
		u_u.put("yes", 0.8);
		system.addUserInput(u_u);

		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		assertEquals("Ground(MakeOffer)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("one single ticket", 0.9);
		system.addUserInput(u_u);
		assertEquals(0.9, system.getContent("a_u").getProb("[Inform(Number,1)]"),
				0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("NbTickets"),
				0.01);
		assertEquals("Ground(MakeOffer)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Confirm(NbTickets,1)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		u_u.clear();
		u_u.put("yes thank you", 1.0);
		system.addUserInput(u_u);
		assertEquals(1, system.getContent("a_u").getValues().size(), 0.01);
		assertEquals(1.0, system.getContent("a_u").getProb("[Confirm]"), 0.05);
		assertEquals(1.0, system.getContent("NbTickets").getProb(1), 0.05);
		assertEquals("Confirm(NbTickets,1)",
				system.getContent("a_m-prev").toDiscrete().getBest().toString());
		assertEquals("Ground(NbTickets,1)",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertEquals(1.0, system.getContent("current_step").getProb("LastConfirm"),
				0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("Shall I confirm"));
		assertTrue(
				system.getContent("u_m").getBest().toString().contains("299 EUR"));
		u_u.clear();
		u_u.put("yes please", 0.5);
		u_u.put("yellow", 0.4);
		system.addUserInput(u_u);
		assertEquals(0.9397, system.getContent("a_u").getProb("[Confirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("Final"), 0.01);
		assertEquals("Book",
				system.getContent("a_m").toDiscrete().getBest().toString());
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("additional tickets?"));
		DialogueState copystate3 = system.getState().copy();
		u_u.clear();
		u_u.put("thanks but no thanks", 0.7);
		system.addUserInput(u_u);
		assertEquals(0.97, system.getContent("a_u").getProb("[Disconfirm]"), 0.01);
		assertEquals(1.0, system.getContent("current_step").getProb("Close"), 0.01);
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("welcome back!"));
		assertTrue(system.getModule(DialogueRecorder.class).getRecord()
				.contains("<systemTurn><variable id=\"u_m\">"
						+ "<value>You are ordering one round-trip ticket from Moss to Tromsø on March 16 and returning on "
						+ "March 20 for a total cost of 299 EUR. "
						+ "Shall I confirm your order?</value></variable>"));
		assertEquals(
				Arrays.asList("Date", "Departure", "Destination", "NbTickets",
						"ReturnDate", "TotalCost", "a_m", "a_m-prev", "a_u",
						"current_step", "u_m", "u_u"),
				system.getState().getChanceNodeIds().stream().sorted()
						.collect(Collectors.toList()));

		assertEquals(1,
				system.getContent(Arrays.asList("Date", "Departure", "Destination",
						"NbTickets", "ReturnDate", "TotalCost")).toDiscrete()
				.getValues().size());
		assertEquals(1.0,
				system.getContent(Arrays.asList("Date", "Departure", "Destination",
						"NbTickets", "ReturnDate", "TotalCost"))
				.toDiscrete()
				.getProb(new Assignment(new Assignment("Date", "March 16"),
						new Assignment("Departure", "Moss"),
						new Assignment("Destination", "Tromsø"),
						new Assignment("NbTickets", 1),
						new Assignment("ReturnDate", "March 20"),
						new Assignment("TotalCost", 299))),
				0.01);
		system.getState().removeNodes(system.getState().getChanceNodeIds());
		system.getState().addNetwork(copystate3);
		u_u.clear();
		u_u.put("yes", 0.7);
		system.addUserInput(u_u);
		assertFalse(system.getState().hasChanceNode("Destination"));
		assertTrue(system.getContent("u_m").getBest().toString()
				.contains("destination?"));
		assertEquals(
				Arrays.asList("Destination^p", "a_m-prev", "current_step", "u_m",
						"u_u"),
				system.getState().getChanceNodeIds().stream().sorted()
						.collect(Collectors.toList()));
		system.addUserInput("Oslo");
		assertEquals("Ground(Destination,Oslo)",
				system.getContent("a_m").getBest().toString());

	}
}
