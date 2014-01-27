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

package opendial.gui;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings.Recording;
import opendial.bn.BNetwork;
import opendial.domains.Domain;
import opendial.modules.DialogueImporter;
import opendial.modules.DialogueRecorder;
import opendial.modules.WizardControl;
import opendial.modules.WizardLearner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.readers.XMLStateReader;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-01-16 02:21:14 #$
 *
 */
@SuppressWarnings("serial")
public class GUIMenuBar extends JMenuBar {

	public static final String OPENDIAL_DOC = "https://code.google.com/p/opendial/w/list";
	
	// logger
	public static Logger log = new Logger("ToolkitMenu", Logger.Level.DEBUG);

	GUIFrame frame;
	JMenuItem exportState;
	JMenuItem exportParams;
	JMenuItem stateDisplayMenu;
	
	public GUIMenuBar(final GUIFrame frame) {
		this.frame = frame;
		JMenu domainMenu = new JMenu("Domain");
		JMenuItem openDomain = new JMenuItem("Open Domain");
		openDomain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				openDomain();
			}
		});
		domainMenu.add(openDomain);
		
		domainMenu.add(new JSeparator());

		JMenu importMenu = new JMenu("Import");
		domainMenu.add(importMenu);
		final JMenuItem importState = new JMenuItem("Dialogue State");
		importState.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				importAction("state");
			}
		});
		importMenu.add(importState);
		
		final JMenuItem importParams = new JMenuItem("Parameters");
		importParams.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				importAction("parameters");				
			}
		});
		importMenu.add(importParams);
		
		JMenu exportMenu = new JMenu("Export");
		domainMenu.add(exportMenu);
		exportState = new JMenuItem("Dialogue State");
		exportState.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				exportAction("state");
			}
		});
		exportMenu.add(exportState);
		
		exportParams = new JMenuItem("Parameters");
		exportParams.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				exportAction("parameters");
			}
		});
		exportMenu.add(exportParams);
				
		domainMenu.add(new JSeparator());
		final JMenuItem exit = new JMenuItem("Close OpenDial");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				System.exit(0);
			}
		});
		
		domainMenu.add(exit);
		add(domainMenu);



		JMenu traceMenu = new JMenu("Interaction");

		JMenuItem freezeItem = new JMenuItem("Pause/Resume");
		freezeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().pause(!frame.getSystem().isPaused());
			}
		});
		traceMenu.add(freezeItem);

		
		JMenu modeMenu = new JMenu("Interaction mode");
		ButtonGroup modeGroup = new ButtonGroup();
		JRadioButtonMenuItem normalMode = new JRadioButtonMenuItem("Normal mode");
		normalMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				switchMode(false);
			}
		});
		JRadioButtonMenuItem wozMode = new JRadioButtonMenuItem("Wizard-of-Oz mode");
		wozMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				switchMode(true);
			}
		});
		modeGroup.add(normalMode);
		modeGroup.add(wozMode);
		normalMode.setSelected(true);
		modeMenu.add(normalMode);
		modeMenu.add(wozMode);
		traceMenu.add(modeMenu);

		traceMenu.add(new JSeparator());

		JMenuItem runThrough = new JMenuItem("Import Dialogue From...");

		runThrough.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				importInteraction();
			}});
		traceMenu.add(runThrough);
		
		
		final JMenuItem saveInteraction = new JMenuItem("Save Dialogue As...");
		saveInteraction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				saveInteraction();
			}});
		traceMenu.add(saveInteraction);
	
		add(traceMenu);
		JMenu optionMenu = new JMenu("Options"); 
		JMenu interactionMenu = new JMenu("View Utterances");
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem singleBest = new JRadioButtonMenuItem("Single-best");
		singleBest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getChatTab().setNBest(1);
				frame.addComment("Number of shown user hypotheses: 1");
			}
		});
		JRadioButtonMenuItem threeBest = new JRadioButtonMenuItem("3-best list");
		threeBest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getChatTab().setNBest(3);
				frame.addComment("Number of shown user hypotheses: 3");
			}
		});
		JRadioButtonMenuItem allBest = new JRadioButtonMenuItem("Full N-best list");
		allBest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getChatTab().setNBest(20);
				frame.addComment("Number of shown user hypotheses: 20");
			}
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
		none.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().getSettings().recording = Recording.NONE;
				frame.addComment("Stop recording intermediate dialogue states");
			}
		});
		JRadioButtonMenuItem last = new JRadioButtonMenuItem("Last input");
		last.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().getSettings().recording = Recording.LAST_INPUT;
				frame.addComment("Recording intermediate dialogue states for the last user input");
			}
		});
		JRadioButtonMenuItem all = new JRadioButtonMenuItem("Full history");
		all.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().getSettings().recording = Recording.ALL;
				frame.addComment("Recording all intermediate dialogue states (warning: can slow down processing)");
			}
		});
		group2.add(none);
		group2.add(last);
		group2.add(all);
		switch (frame.getSystem().getSettings().recording) {
		case NONE : none.setSelected(true); break;
		case LAST_INPUT : last.setSelected(true); break;
		case ALL : all.setSelected(true); break;
		}
		recording.add(none);
		recording.add(last);
		recording.add(all);
		optionMenu.add(recording);
		
		stateDisplayMenu = new JMenuItem("Show/Hide parameters");
		stateDisplayMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				frame.getStateViewerTab().showParameters(!frame.getStateViewerTab().showParameters());
				frame.addComment("Show parameters: " + frame.getStateViewerTab().showParameters());
			}
		});
		optionMenu.add(stateDisplayMenu);
		optionMenu.add(new JSeparator());

		
		JMenuItem modules = new JMenuItem("Load Modules");
		modules.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				new ModulesPanel(frame);
			}
		});
		optionMenu.add(modules);

		optionMenu.add(new JSeparator());
		
		JMenuItem config = new JMenuItem("Settings");
		config.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				new SettingsPanel(frame);
			}
		});
		optionMenu.add(config);
		

	
		
		add(optionMenu);
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				showAboutPanel(frame);
			}
		});
		
		
		JMenuItem docItem = new JMenuItem("Documentation");
		docItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				openDocumentation();
			}
		});
		helpMenu.add(aboutItem);
		helpMenu.add(docItem);
		add(helpMenu);
	}

	

	protected void switchMode(boolean isWozMode) {
		if (isWozMode) {
			frame.getSystem().attachModule(WizardControl.class);
				frame.addComment("Switching interaction to Wizard-of-Oz mode");

		}
		else {
			frame.getSystem().detachModule(WizardControl.class);
			frame.addComment("Switching interaction to normal mode");
		}
	}


	protected void importInteraction() {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String interactionFile = fc.getSelectedFile().getAbsolutePath();
			frame.addComment("Importing interaction " + interactionFile);
			try {
			List<DialogueState> interaction = XMLInteractionReader.extractInteraction(interactionFile);
			DialogueImporter importer = new DialogueImporter(frame.getSystem(), interaction);
			importer.start();
			}
			catch (Exception f) {
				log.warning("could not extract interaction: " + f);
				frame.addComment(f.toString());
			}
		}
	}

	

	protected void openDocumentation() {
		if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("https://code.google.com/p/opendial/w/list"));
            } catch (Exception e1) {
                e1.printStackTrace();
            } 
        }
	}


	protected void showAboutPanel(GUIFrame frame) {
		try {
			BufferedImage original = ImageIO.read(new File(GUIFrame.ICON_PATH));
			
			JLabel label = new JLabel();
			Font font = label.getFont();

		    // create some css from the label's font
		    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		    style.append("font-size:" + font.getSize() + "pt;");
		    
			JEditorPane ep = new JEditorPane("text/html","<html><body style=\"" + style 
					+ "\"><b>OpenDial dialogue toolkit, version 0.9</b><br>"
					+ "Copyright (C) 2011-2015 by Pierre Lison<br>University of Oslo, Norway<br><br>"
					+ "OpenDial is free software; you can redistribute it and/or<br>" 
					+ "modify it under the terms of the GNU Lesser General Public<br>"
					+ "License as published by the Free Software Foundation.<br><br>"
					+ "<i>Project website</i>: <a href=\"http://opendial.googlecode.com\">"
					+ "http://opendial.googlecode.com</a><br>"
					+ "<i>Contact</i>: Pierre Lison (email: <a href=\"mailto:plison@ifi.uio.no\">"
					+ "plison@ifi.uio.no</a>)</body></html>");
			
			 // handle link events
		    ep.addHyperlinkListener(new HyperlinkListener()
		    {
		        @Override
				public void hyperlinkUpdate(HyperlinkEvent e)  {
		            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED) && Desktop.isDesktopSupported()) {
		                    try {
		                        Desktop.getDesktop().browse(e.getURL().toURI());
		                    } catch (Exception e1) {
		                        e1.printStackTrace();
		                    } 
		                }
		        }
		    });
		    ep.setEditable(false);
		    ep.setBackground(label.getBackground());

		JOptionPane.showMessageDialog(frame.getFrame(), ep ,
				"About OpenDial", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(original));
		}
		catch (Exception f) {
			log.warning("could not show about box: " + f);
		}
	}

	protected void saveInteraction() {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String recordFile = fc.getSelectedFile().getAbsolutePath();
			frame.getSystem().getModule(DialogueRecorder.class).writeToFile(recordFile);
			frame.addComment("Interaction saved to " + recordFile);
		}
	}
	
	protected void importAction (String tag) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String stateFile = fc.getSelectedFile().getAbsolutePath();
			frame.addComment("Importing " + tag + " from "  + stateFile);
			try {
				importAction(frame.getSystem(), stateFile, tag);
			}
			catch (Exception f) {
				log.warning("could not extract interaction: " + f);
				frame.addComment(f.toString());
			}
		}
	}
	
	public static void importAction(DialogueSystem system, String file, String tag) throws DialException {
		if (tag.equals("parameters")) {
			BNetwork parameters = XMLStateReader.extractBayesianNetwork(file, tag);
			for (String oldParam : system.getState().getParameterIds()) {
				if (!parameters.hasChanceNode(oldParam)) {
				parameters.addNode(system.getState().getChanceNode(oldParam));
				}
			}
			system.getState().setParameters(parameters);
		}
		else {
			BNetwork state = XMLStateReader.extractBayesianNetwork(file, tag);
			system.addContent(new DialogueState(state));
		}
	}

	protected void exportAction(String tag) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String recordFile = fc.getSelectedFile().getAbsolutePath();
				exportAction(frame.getSystem(), recordFile, tag);
				frame.addComment(tag.substring(0,1).toUpperCase() + tag.substring(1) + " saved to " + recordFile);
			}
			catch (DialException j) {
				log.warning("could not save parameter distribution: " + j);
			}
		}
	}
	
	public static void exportAction(DialogueSystem system, String file, String tag) throws DialException {
		Document doc = XMLUtils.newXMLDocument();
		
		Set<String> parameterIds = new HashSet<String>(system.getState().getParameterIds());
		Set<String> otherVarsIds = new HashSet<String>(system.getState().getChanceNodeIds());
		otherVarsIds.removeAll(parameterIds);
		Set<String> variables = (tag.equals("parameters"))? parameterIds : otherVarsIds;
		Node paramXML = system.getState().generateXML(doc, variables);
		doc.renameNode(paramXML, null, tag);
		doc.appendChild(paramXML);
		XMLUtils.writeXMLDocument(doc, file);
	}



	protected void openDomain() {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showOpenDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String domainFile = fc.getSelectedFile().getAbsolutePath();
				Domain domain = XMLDomainReader.extractDomain(domainFile);
				frame.getSystem().changeDomain(domain);
				frame.addComment("Now using domain: " + domainFile);
			}
			catch(DialException j) {
				frame.addComment("Cannot use domain: " + j);
			}	            
		} 
	}



	public void trigger() {
		Set<String> parameterIds = new HashSet<String>(frame.getSystem().getState().getParameterIds());
		Set<String> otherVarsIds = new HashSet<String>(frame.getSystem().getState().getChanceNodeIds());
		otherVarsIds.removeAll(parameterIds);
		exportState.setEnabled(!otherVarsIds.isEmpty());		
		exportParams.setEnabled(!parameterIds.isEmpty());
		stateDisplayMenu.setEnabled(!parameterIds.isEmpty());
	}


}
