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
import java.util.List;

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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ArrayVal;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.modules.DialogueRecorder;
import opendial.modules.DialogueImporter;
import opendial.modules.WizardControl;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;


/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ToolkitMenu extends JMenuBar {

	public static final String ICON_PATH = "resources/opendial-icon.png";
	public static final String OPENDIAL_DOC = "https://code.google.com/p/opendial/w/list";
	
	// logger
	public static Logger log = new Logger("ToolkitMenu", Logger.Level.DEBUG);

	public ToolkitMenu(final GUIFrame frame) {
		JMenu domainMenu = new JMenu("Domain");
		JMenuItem openDomain = new JMenuItem("Open Domain");
		final JMenuItem saveParamsAs = new JMenuItem("Save Parameter Estimates As...");
		openDomain.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				openDomain(frame);
				saveParamsAs.setEnabled(!frame.getSystem().getState().getParameterIds().isEmpty());
			}
		});
		saveParamsAs.setEnabled(!frame.getSystem().getState().getParameterIds().isEmpty());
		saveParamsAs.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				saveParameters(frame);
			}});
		domainMenu.add(openDomain);
		domainMenu.add(saveParamsAs);
		add(domainMenu);


		JMenu traceMenu = new JMenu("Interaction");

		JMenuItem freezeItem = new JMenuItem("Pause/Resume");
		freezeItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				frame.getSystem().pause(!frame.getSystem().isPaused());
			}
		});
		traceMenu.add(freezeItem);

		
		JMenu modeMenu = new JMenu("Interaction mode");
		ButtonGroup modeGroup = new ButtonGroup();
		JRadioButtonMenuItem normalMode = new JRadioButtonMenuItem("Normal mode");
		normalMode.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				switchMode(frame, false);
			}
		});
		JRadioButtonMenuItem wozMode = new JRadioButtonMenuItem("Wizard-of-Oz mode");
		wozMode.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				switchMode(frame, true);
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
			public void actionPerformed (ActionEvent e) {
				importInteraction(frame);
			}});
		traceMenu.add(runThrough);
		
		
		final JMenuItem saveInteraction = new JMenuItem("Save Dialogue As...");
		saveInteraction.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				saveInteraction(frame);
			}});
		traceMenu.add(saveInteraction);
	
		add(traceMenu);
		JMenu optionMenu = new JMenu("Options"); 
		JMenu interactionMenu = new JMenu("User Utterances");
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem singleBest = new JRadioButtonMenuItem("Single-best");
		singleBest.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				frame.getChatTab().setNBest(1);
				frame.addComment("Number of shown user hypotheses: 1");
			}
		});
		JRadioButtonMenuItem threeBest = new JRadioButtonMenuItem("3-best list");
		threeBest.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				frame.getChatTab().setNBest(3);
				frame.addComment("Number of shown user hypotheses: 3");
			}
		});
		JRadioButtonMenuItem allBest = new JRadioButtonMenuItem("Full N-best list");
		allBest.addActionListener(new ActionListener() {
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
		JMenuItem stateDisplayMenu = new JMenuItem("Show/Hide parameters");
		stateDisplayMenu.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				frame.getStateViewerTab().showParameters(!frame.getStateViewerTab().showParameters());
				frame.addComment("Show parameters: " + frame.getStateViewerTab().showParameters());
			}
		});
		optionMenu.add(stateDisplayMenu);

		JMenuItem config = new JMenuItem("Settings");
		config.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				new SettingsPanel(frame);
			}
		});
		optionMenu.add(new JSeparator());
		optionMenu.add(config);
		add(optionMenu);
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				showAboutPanel(frame);
			}
		});
		
		
		JMenuItem docItem = new JMenuItem("Documentation");
		docItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				openDocumentation();
			}
		});
		helpMenu.add(aboutItem);
		helpMenu.add(docItem);
		add(helpMenu);
	}

	

	protected void switchMode(GUIFrame frame, boolean isWozMode) {
		if (isWozMode) {
				frame.getSystem().attachModule(new WizardControl(), true);
				frame.addComment("Switching interaction to Wizard-of-Oz mode");
		}
		else {
			frame.getSystem().detachModule(frame.getSystem().getModule(WizardControl.class));
			frame.addComment("Switching interaction to normal mode");
		}
	}


	protected void importInteraction(GUIFrame frame) {
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
			BufferedImage original = ImageIO.read(new File(ICON_PATH));
			
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

	protected void saveInteraction(GUIFrame frame) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String recordFile = fc.getSelectedFile().getAbsolutePath();
			frame.getSystem().getModule(DialogueRecorder.class).writeToFile(recordFile);
			frame.addComment("Interaction saved to " + recordFile);
		}
	}

	protected void saveParameters(GUIFrame frame) {
		final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int returnVal = fc.showSaveDialog(frame.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String recordFile = fc.getSelectedFile().getAbsolutePath();
				Document doc = XMLUtils.newXMLDocument();
				DialogueState curState = frame.getSystem().getState();
				Element root = doc.createElement("parameters");
				for (String param : curState.getParameterIds()) {
					ChanceNode cn = curState.getChanceNode(param);
					try {
					ContinuousDistribution distrib = cn.getDistrib().getPosterior(new Assignment()).toContinuous();
					Double[] mean = distrib.getFunction().getMean();
					Element var = doc.createElement("variable");
					Attr id = doc.createAttribute("id");
					id.setValue(param);
					var.setAttributeNode(id);
					Element valueNode = doc.createElement("value");
					valueNode.setTextContent(new ArrayVal(mean).toString());
					var.appendChild(valueNode);
					root.appendChild(var);
					}
					catch (DialException f) {
						log.warning("cannot write parameter estimate: " + f);
					}
				}
				doc.appendChild(root);
				XMLUtils.writeXMLDocument(doc, recordFile);
				frame.addComment("Parameter estimates saved to " + recordFile);
			}
			catch (DialException j) {
				log.warning("could not save parameter distribution: " + j);
			}
		}
	}

	protected void openDomain(GUIFrame frame) {
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


}
