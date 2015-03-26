
INSTALLATION
============

This plugin allows you to use the MARY Text-to-Speech engine for performing
speech synthesis in your dialogue system.

To install the plugin, do the following:

1) add the three JAR files marytts-lang-en-5.1.jar, marytts-runtime-5.1-jar-with-dependencies.jar and
   voice-cmu-slt-hsmm-5.1.jar to  ./lib directory
2) add the java file MaryTTs.java to the ./src/opendial/plugins directory
3) recompile OpenDial.
	

START
============

The plugin does not require any particular parameter.  To use the plugin at runtime, 
you simply need to load the opendial.plugins.MaryTTs module [1].
	
The easiest way to use the plugin is to add the following to your domain specification:

	<settings>
		<modules>opendial.plugins.MaryTTS</modules>
	</settings>

If you want to change the language or voice for the speech synthesis, read the documentation
available on 
	https://github.com/marytts/marytts/wiki/MaryInterface 
	and make the necessary changes in the MaryTTS module.  


[1] See the online documentation for more details on how to attach/detach modules.