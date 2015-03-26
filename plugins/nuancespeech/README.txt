
INSTALLATION
============

To install the plugin for the Nuance Speech API, do the following:

1) add the JAR file apache-hc.jar to the ./lib directory
2) add the NuanceSpeech.java file to the ./src/opendial/plugins directory
3) recompile OpenDial.

The plugin requires an application ID and key. To obtain these, you first need to 
register as a developer on the Nuance Mobile Developer website:
	http://dragonmobile.nuancemobiledeveloper.com

Once registered, go to " View Sandbox credentials", and you will find your AppID and 
key (use the one for HTTP Client Applications) necessary to use the plugin.  You can
upload custom vocabularies by following the link "upload and manage vocabularies".

START
============

The plugin relies on three parameters:
- "id" (mandatory) is the Sandbox application ID
- "key" (mandatory) is the Sandbox application key
- "lang" (mandatory) is the language code for your application (e.g. eng-USA, nor-NOR, etc.)

To use the plugin at runtime, you simply need to load the opendial.plugins.NuanceSpeech 
module [1].

You need to make sure that the parameters mentioned above are set when starting the 
system. The easiest way is to add the following to your domain specification:

	<settings>
		<modules>opendial.plugins.NuanceSpeech</modules>
		<id>add here your application ID</id>
		<key>add here your application key</secret>
	</settings>

For improved speech recognition results, it is recommended to use custom vocabularies 
tailored for your application.  Check the Nuance developer website on how to upload 
and activate such custom vocabularies. 

AUDIO SETUP
============

You can change the particular input and output mixers used at runtime for the
speech recognition and synthesis through the "Options" menu of OpenDial.


[1] See the online documentation for more details on how to attach/detach modules.