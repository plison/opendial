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

package opendial.gui.utils;

import java.util.logging.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import opendial.DialogueState;
import opendial.Settings;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.UtilityFunction;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechData;
import opendial.modules.AudioModule;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

/**
 * Mouse plugin to handle the right-click popup after selecting nodes
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class PopupHandler extends AbstractPopupGraphMousePlugin
		implements MouseListener {

	// logger
	public final static Logger log = Logger.getLogger("OpenDial");

	private StateViewer viewer;
	List<String> speechVars;
	AudioModule audio;

	/**
	 * Constructs the popup handler for the state viewer.
	 * 
	 * @param viewer the state viewer component.
	 */
	public PopupHandler(StateViewer viewer) {
		super(InputEvent.BUTTON3_MASK);
		this.viewer = viewer;
		Settings settings = viewer.getSystem().getSettings();
		speechVars = Arrays.asList(settings.userSpeech, settings.systemSpeech);
		audio = viewer.getSystem().getModule(AudioModule.class);
	}

	/**
	 * Creates the popup with relevant action items
	 *
	 * @param e the mouse event
	 */
	@Override
	protected void handlePopup(MouseEvent e) {
		super.mouseClicked(e);

		List<String> pickedVertices = getPickedVertices();
		DialogueState state = viewer.getState();

		JPopupMenu popup = new JPopupMenu();
		if (!pickedVertices.isEmpty() && state.hasChanceNodes(pickedVertices)) {
			JMenuItem marginalItem =
					new JMenuItem("Calculate marginal distribution");

			marginalItem.addActionListener(ev -> {
				MultivariateDistribution distrib = state.queryProb(pickedVertices);
				viewer.getStateMonitorTab().writeToLogArea(distrib);
				resetPickedVertices();
			});

			popup.add(marginalItem);
		}
		if (pickedVertices.size() == 1
				&& state.hasChanceNode(pickedVertices.get(0))) {
			JMenuItem distribItem = new JMenuItem("Show distribution chart");

			distribItem.addActionListener(ev -> {
				viewer.displayDistrib(pickedVertices.iterator().next());
				resetPickedVertices();
			});

			popup.add(distribItem);
		}
		if (pickedVertices.size() == 1
				&& speechVars.contains(pickedVertices.get(0))) {
			JMenuItem playItem = new JMenuItem("Play sound");

			playItem.addActionListener(ev -> {
				String speechVar = pickedVertices.iterator().next();
				Value v = viewer.getState().queryProb(speechVar).getBest();
				if (audio != null && v instanceof SpeechData) {
					audio.playSpeech((SpeechData) v);
				}
			});

			popup.add(playItem);
		}
		if (!pickedVertices.isEmpty() && !state.getUtilityNodeIds().isEmpty()) {
			JMenuItem utilityItem = new JMenuItem("Calculate utility");

			utilityItem.addActionListener(ev -> {

				UtilityFunction result = viewer.getState().queryUtil(pickedVertices);
				viewer.getStateMonitorTab().writeToLogArea(result);
				resetPickedVertices();
			});

			popup.add(utilityItem);

		}

		// other action: draw outgoing dependency

		if (popup.getComponentCount() == 0 && !pickedVertices.isEmpty()) {
			popup.add(
					new JLabel("  No action available for the selected node(s)  "));
		}
		if (popup.getComponentCount() > 0) {
			popup.show(viewer, e.getX(), e.getY());
		}
	}

	private List<String> getPickedVertices() {
		List<String> pickedVertices = new LinkedList<String>();
		DialogueState state = viewer.getState();
		if (state == null) {
			return pickedVertices;
		}
		for (String vertice : viewer.getPickedVertexState().getPicked()) {
			if (viewer.getBNode(vertice) != null) {
				pickedVertices.add(viewer.getBNode(vertice).getId());
			}
		}
		pickedVertices.removeAll(state.getUtilityNodeIds());
		return pickedVertices;
	}

	private void resetPickedVertices() {
		for (String queryVariable : new LinkedList<String>(getPickedVertices())) {
			viewer.getPickedVertexState().pick(queryVariable, false);
		}
	}

}
