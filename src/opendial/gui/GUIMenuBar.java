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

package opendial.gui;

import java.util.logging.*;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.sound.sampled.Mixer;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;

import opendial.Settings;
import opendial.Settings.Recording;
import opendial.modules.AudioModule;
import opendial.utils.AudioUtils;

/**
 * Menu bar for the GUI.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
@SuppressWarnings("serial")
public class GUIMenuBar extends JMenuBar {

	public static final String OPENDIAL_DOC =
			"http://www.opendial-toolkit.net/user-manual";

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	GUIFrame frame;
	JMenuItem saveDomain;
	JMenuItem saveDomainAs;
	JMenuItem resetItem;
	JMenuItem freezeItem;
	JMenuItem exportState;
	JMenuItem exportParams;
	JMenuItem inputMenu;
	JMenuItem outputMenu;
	JMenuItem stateDisplayMenu;

	JRadioButtonMenuItem systemRole;

	/**
	 * Creates the menu bar for the frame.
	 * 
	 * @param frame the frame to which the menu bar is attached.
	 */
	public GUIMenuBar(final GUIFrame frame) {
		this.frame = frame;
		JMenu domainMenu = new JMenu("Domain");
		JMenuItem newDomain = new JMenuItem("New");
		newDomain.addActionListener(e -> frame.newDomain());
		domainMenu.add(newDomain);

		JMenuItem openDomain = new JMenuItem("Open File");
		openDomain.addActionListener(e -> frame.openDomain());
		domainMenu.add(openDomain);

		saveDomain = new JMenuItem("Save");
		saveDomain.addActionListener(e -> frame.saveDomain());
		KeyStroke keyStrokeToOpen = KeyStroke.getKeyStroke(KeyEvent.VK_S,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		saveDomain.setAccelerator(keyStrokeToOpen);
		domainMenu.add(saveDomain);

		saveDomainAs = new JMenuItem("Save As...");
		saveDomainAs.addActionListener(e -> frame.saveDomainAs());
		domainMenu.add(saveDomainAs);

		domainMenu.add(new JSeparator());

		JMenu importMenu = new JMenu("Import");
		domainMenu.add(importMenu);
		final JMenuItem importState = new JMenuItem("Dialogue State");
		importState.addActionListener(e -> frame.importContent("state"));
		importMenu.add(importState);

		final JMenuItem importParams = new JMenuItem("Parameters");
		importParams.addActionListener(e -> frame.importContent("parameters"));
		importMenu.add(importParams);

		JMenu exportMenu = new JMenu("Export");
		domainMenu.add(exportMenu);
		exportState = new JMenuItem("Dialogue State");
		exportState.addActionListener(e -> frame.exportContent("state"));
		exportMenu.add(exportState);

		exportParams = new JMenuItem("Parameters");
		exportParams.addActionListener(e -> frame.exportContent("parameters"));
		exportMenu.add(exportParams);

		domainMenu.add(new JSeparator());
		final JMenuItem exit = new JMenuItem("Close OpenDial");
		exit.addActionListener(e -> frame.closeWindow());

		domainMenu.add(exit);
		add(domainMenu);

		JMenu traceMenu = new JMenu("Interaction");

		resetItem = new JMenuItem("Reset");
		resetItem.addActionListener(e -> frame.resetInteraction());
		KeyStroke keyStrokeToReset = KeyStroke.getKeyStroke(KeyEvent.VK_R,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		resetItem.setAccelerator(keyStrokeToReset);
		traceMenu.add(resetItem);

		freezeItem = new JMenuItem("Pause/Resume");
		freezeItem.addActionListener(e -> {
			boolean toPause = !frame.getSystem().isPaused();
			frame.getSystem().pause(toPause);
			frame.getSystem()
					.displayComment((toPause) ? "system paused" : "system resumed");
		});
		KeyStroke keyStrokeToPause = KeyStroke.getKeyStroke(KeyEvent.VK_P,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		freezeItem.setAccelerator(keyStrokeToPause);
		traceMenu.add(freezeItem);

		traceMenu.add(new JSeparator());

		JMenuItem connect = new JMenuItem("Connect to Remote Client");
		connect.addActionListener(e -> {
			String fullAddress = JOptionPane.showInputDialog(this,
					"Enter address of remote client (IP_address:port):");
			if (fullAddress != null && fullAddress.contains(":")) {
				String ipaddress = fullAddress.split(":")[0];
				int port = Integer.parseInt(fullAddress.split(":")[1]);
				frame.getSystem().connectTo(ipaddress, port);
			}
			else if (fullAddress != null) {
				frame.getSystem().displayComment("address of remote client is "
						+ "not well-formed, must be \"IP_address:port\"");
			}
		});

		traceMenu.add(connect);

		JMenu roleMenu = new JMenu("Interaction role");
		ButtonGroup modeGroup = new ButtonGroup();
		JRadioButtonMenuItem userRole = new JRadioButtonMenuItem("User");
		ItemListener inversion = e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				Settings settings = frame.getSystem().getSettings();
				settings.invertedRole = !settings.invertedRole;
			}
		};
		modeGroup.add(userRole);
		userRole.setSelected(true);
		roleMenu.add(userRole);
		systemRole = new JRadioButtonMenuItem("System");
		modeGroup.add(systemRole);
		roleMenu.add(systemRole);

		traceMenu.add(roleMenu);
		userRole.addItemListener(inversion);
		systemRole.addItemListener(inversion);

		traceMenu.add(new JSeparator());

		JMenu runThrough = new JMenu("Import Dialogue From...");
		JMenuItem normal = new JMenuItem("Normal Transcript");
		JMenuItem woz = new JMenuItem("Wizard-of-Oz Transcript");
		runThrough.add(normal);
		runThrough.add(woz);
		normal.addActionListener(e -> frame.importInteraction(false));
		woz.addActionListener(e -> frame.importInteraction(true));
		traceMenu.add(runThrough);

		final JMenuItem saveInteraction = new JMenuItem("Save Dialogue As...");
		saveInteraction.addActionListener(e -> frame.saveInteraction());
		traceMenu.add(saveInteraction);

		add(traceMenu);
		JMenu optionMenu = new JMenu("Options");

		inputMenu = new JMenu("Audio input");
		ButtonGroup inputGroup = new ButtonGroup();
		List<Mixer.Info> mixers = AudioUtils.getInputMixers();
		for (final Mixer.Info mixer : mixers) {
			JRadioButtonMenuItem mixerButton =
					new JRadioButtonMenuItem(mixer.getName());
			mixerButton.addActionListener(e -> {
				frame.getSystem().getSettings().inputMixer = mixer;
				if (frame.getSystem().getModule(AudioModule.class) != null) {
					frame.getSystem().getModule(AudioModule.class).start();
				}
			});
			inputGroup.add(mixerButton);
			inputMenu.add(mixerButton);
			if (mixer.equals(frame.getSystem().getSettings().inputMixer)) {
				mixerButton.setSelected(true);
			}
		}

		optionMenu.add(inputMenu);

		outputMenu = new JMenu("Audio output");
		ButtonGroup outputGroup = new ButtonGroup();
		for (final Mixer.Info mixer : AudioUtils.getOutputMixers()) {
			JRadioButtonMenuItem mixerButton =
					new JRadioButtonMenuItem(mixer.getName());
			mixerButton.addActionListener(
					e -> frame.getSystem().getSettings().outputMixer = mixer);

			outputGroup.add(mixerButton);
			outputMenu.add(mixerButton);
			if (mixer.equals(frame.getSystem().getSettings().outputMixer)) {
				mixerButton.setSelected(true);
			}
		}

		optionMenu.add(outputMenu);

		JMenu interactionMenu = new JMenu("View Utterances");
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem singleBest = new JRadioButtonMenuItem("Single-best");
		singleBest.addActionListener(e -> {
			frame.getChatTab().setNBest(1);
			frame.addComment("Number of shown user hypotheses: 1");
		});

		JRadioButtonMenuItem threeBest = new JRadioButtonMenuItem("3-best list");
		threeBest.addActionListener(e -> {
			frame.getChatTab().setNBest(3);
			frame.addComment("Number of shown user hypotheses: 3");
		});
		JRadioButtonMenuItem allBest = new JRadioButtonMenuItem("Full N-best list");
		allBest.addActionListener(e -> {
			frame.getChatTab().setNBest(20);
			frame.addComment("Number of shown user hypotheses: 20");
		});

		group.add(singleBest);
		group.add(threeBest);
		group.add(allBest);
		allBest.setSelected(true);
		interactionMenu.add(singleBest);
		interactionMenu.add(threeBest);
		interactionMenu.add(allBest);
		optionMenu.add(interactionMenu);

		JMenu recording = new JMenu("Record Intermediate States");
		ButtonGroup group2 = new ButtonGroup();
		JRadioButtonMenuItem none = new JRadioButtonMenuItem("None");
		none.addActionListener(e -> {
			frame.getSystem().getSettings().recording = Recording.NONE;
			frame.addComment("Stop recording intermediate dialogue states");
		});

		JRadioButtonMenuItem last = new JRadioButtonMenuItem("Last input");
		last.addActionListener(e -> {
			frame.getSystem().getSettings().recording = Recording.LAST_INPUT;
			frame.addComment(
					"Recording intermediate dialogue states for the last user input");
		});

		JRadioButtonMenuItem all = new JRadioButtonMenuItem("Full history");
		all.addActionListener(e -> {
			frame.getSystem().getSettings().recording = Recording.ALL;
			frame.addComment("Recording all intermediate dialogue states "
					+ "(warning: can slow down processing)");
		});

		group2.add(none);
		group2.add(last);
		group2.add(all);
		switch (frame.getSystem().getSettings().recording) {
		case NONE:
			none.setSelected(true);
			break;
		case LAST_INPUT:
			last.setSelected(true);
			break;
		case ALL:
			all.setSelected(true);
			break;
		}
		recording.add(none);
		recording.add(last);
		recording.add(all);
		optionMenu.add(recording);

		stateDisplayMenu = new JMenuItem("Show/Hide parameters");
		stateDisplayMenu.addActionListener(e -> {
			boolean curSetting = frame.getStateViewerTab().showParameters();
			frame.getStateViewerTab().showParameters(!curSetting);
			frame.addComment("Show parameters: "
					+ frame.getStateViewerTab().showParameters());
		});

		optionMenu.add(stateDisplayMenu);

		optionMenu.add(new JSeparator());

		JMenuItem config = new JMenuItem("Settings");
		config.addActionListener(e -> new SettingsPanel(frame));
		optionMenu.add(config);

		add(optionMenu);
		JMenu helpMenu = new JMenu("Help");

		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(e -> showAboutPanel());

		JMenuItem docItem = new JMenuItem("Documentation");
		docItem.addActionListener(e -> openDocumentation());

		helpMenu.add(aboutItem);
		helpMenu.add(docItem);
		add(helpMenu);
	}

	/**
	 * Enables or disables the speech option menu
	 * 
	 * @param toEnable true if speech should be enabled, else false
	 */
	protected void enableSpeech(boolean toEnable) {
		inputMenu.setEnabled(toEnable);
	}

	/**
	 * Opens the documentation on the project website
	 */
	protected void openDocumentation() {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI(OPENDIAL_DOC));
			}
			catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Displays the "About" panel.
	 * 
	 */
	protected void showAboutPanel() {
		try {
			BufferedImage original = ImageIO.read(new File(GUIFrame.ICON_PATH));

			JLabel label = new JLabel();
			Font font = label.getFont();

			// create some css from the label's font
			StringBuffer style =
					new StringBuffer("font-family:" + font.getFamily() + ";");
			style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
			style.append("font-size:" + font.getSize() + "pt;");

			JEditorPane ep = new JEditorPane("text/html", "<html><body style=\""
					+ style + "\"><b>OpenDial dialogue toolkit, version 1.4</b><br>"
					+ "Copyright (C) 2011-2015 by Pierre Lison<br>University of Oslo, Norway<br><br>"
					+ "OpenDial is distributed as free software under<br>"
					+ "the <a href=\"http://opensource.org/licenses/MIT\">MIT free software license</a>.<br><br>"
					+ "<i>Project website</i>: <a href=\"http://opendial-toolkit.net\">"
					+ "http://opendial-toolkit.net</a><br>"
					+ "<i>Contact</i>: Pierre Lison (email: <a href=\"mailto:plison@ifi.uio.no\">"
					+ "plison@ifi.uio.no</a>)<br><br>" + "<b>Local address:</b>: <i>"
					+ frame.getSystem().getLocalAddress() + "</i>"
					+ "</body></html>");

			// handle link events
			ep.addHyperlinkListener(e -> {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)
						&& Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			});
			ep.setEditable(false);
			ep.setBackground(label.getBackground());

			JOptionPane.showMessageDialog(frame.getFrame(), ep, "About OpenDial",
					JOptionPane.INFORMATION_MESSAGE, new ImageIcon(original));
		}
		catch (Exception f) {
			log.warning("could not show about box: " + f);
		}
	}

	/**
	 * Updates the menu bar.
	 */
	public void update() {
		Set<String> parameterIds =
				new HashSet<String>(frame.getSystem().getState().getParameterIds());
		Set<String> otherVarsIds =
				new HashSet<String>(frame.getSystem().getState().getChanceNodeIds());
		otherVarsIds.removeAll(parameterIds);
		exportState.setEnabled(!otherVarsIds.isEmpty());
		exportParams.setEnabled(!parameterIds.isEmpty());
		stateDisplayMenu.setEnabled(!parameterIds.isEmpty());

		inputMenu.setEnabled(frame.isSpeechEnabled());
		for (Component c : inputMenu.getComponents()) {
			if (c instanceof JRadioButtonMenuItem
					&& ((JRadioButtonMenuItem) c).getText().startsWith(
							frame.getSystem().getSettings().inputMixer.getName())) {
				((JRadioButtonMenuItem) c).setSelected(true);
			}
		}
		for (Component c : outputMenu.getComponents()) {
			if (c instanceof JRadioButtonMenuItem
					&& ((JRadioButtonMenuItem) c).getText().startsWith(
							frame.getSystem().getSettings().outputMixer.getName())) {
				((JRadioButtonMenuItem) c).setSelected(true);
			}
		}
		systemRole.setEnabled(
				!frame.getSystem().getSettings().remoteConnections.isEmpty());

		boolean realDomain = !frame.getSystem().getDomain().isEmpty();
		saveDomainAs.setEnabled(realDomain);
		boolean isChanged = frame.tabbedPane.getTitleAt(2).contains("*");
		saveDomain.setEnabled(realDomain && isChanged);
		resetItem.setEnabled(realDomain);
		freezeItem.setEnabled(realDomain);

	}

}
