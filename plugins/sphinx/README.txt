
INSTALLATION
============

To install the plugin for the Sphinx 4 speechrecognition engine, do the following:

1) add the JAR files sphinx4.jar and WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz.jar to 
   the ./lib directory
2) add the SphinxASR.java file to the ./src/opendial/plugins directory
3) recompile OpenDial.
	 

START
============

The plugin requires one parameter:
- "grammar" is the path to the recognition grammar in JSGF format

To use the plugin at runtime, you simply need to load the opendial.plugins.SphinxASR 
module [1]. You also need to make sure the grammar parameter is specified in the system 
settings. The easiest way is to add the following to your domain specification:
	<settings>
		<modules>opendial.plugins.SphinxASR</modules>
		<grammar>add here the path to your grammar file</grammar>
	</settings>

An example of JSGF grammar is provided in the file demo.gram.


AUDIO SETUP
============

You can change the particular input and output mixers used at runtime for the
speech recognition and synthesis through the "Options" menu of OpenDial.


[1] See the online documentation for more details on how to attach/detach modules.