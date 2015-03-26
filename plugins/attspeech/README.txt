
INSTALLATION
============

To install the plugin for the AT&T Speech API, do the following:

1) add the JAR files apache-hc.jar, java-json.jar and att-codekit.jar to the ./lib directory
2) add the ATTSpeech.java file to the ./src/opendial/plugins directory
3) recompile OpenDial.

The plugin requires an application ID and secret. To obtain these, you first need to 
register as a developer on the AT&T Developer website:
	https://developer.att.com/apis/speech.

Once registered, you must setup a new application, with access to the "Speech To Text 
Custom" and "Text to Speech" APIs.  At the end of the setup procedure, a confidential 
application ID and secret will be created.  
	

START
============

The plugin relies on three parameters:
- "id" (mandatory) is the AT&T's application ID
- "secret" (mandatory) is the AT&T's application secret
- "grammar" (optional) is the path to the recognition grammar in GRXML format

To use the plugin at runtime, you simply need to load the opendial.plugins.ATTSpeech 
module [1].

You need to make sure that the parameters mentioned above are set when starting the 
system. The easiest way is to add the following to your domain specification:

	<settings>
		<modules>opendial.plugins.ATTSpeech</modules>
		<id>add here your application ID</id>
		<secret>add here your application secret</secret>
		<grammar>add here the path to your grammar file</grammar>
	</settings>

An example of GRXML grammar is provided in the file example-grammar.xml.


AUDIO SETUP
============

You can change the particular input and output mixers used at runtime for the
speech recognition and synthesis through the "Options" menu of OpenDial.


[1] See the online documentation for more details on how to attach/detach modules.