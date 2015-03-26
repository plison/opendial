
INSTALLATION
============

This plugin assumes that you are in possession of a Nao robot (V4) from Aldebaran
Robotics, with the latest software updates.

To install the plugin, do the following:

1) add the JAR file containing the QiMessaging library to the ./lib directory [1]
2) add the java files to the ./src/opendial/plugins directory
3) recompile OpenDial.
	

START
============

The plugin relies on two parameters:
- "nao_ip" (mandatory) is the IP address of the robot
- "grammar" (mandatory) is the path (**on the robot's file system**) of the BNF recognition grammar

To use the plugin at runtime, you simply need to load the Nao... modules [2].  You need to make
sure that the two parameters above are correctly set. The easiest way is to add the following 
to your domain specification:

	<settings>
		<modules>opendial.plugins.NaoASR,opendial.plugins.NaoBehaviour,opendial.plugins.NaoButton,
		         opendial.plugins.NaoPerception,opendial.plugins.NaoTTS</modules>
		<nao_ip>add here the IP address of your robot</nao_ip>
		<grammar>add here the path to the BNF recognition grammar</secret>
	</settings>

An example of BNF grammar is provided in the file grammar.bnf.  For the moment, the grammar must be 
manually uploaded on the robot file system prior to the system initialisation.


[1] The QiMessaging library is a Java bridge for the NaoQi API, available on the community website
    of Aldebaran Robotics (http://community.aldebaran.com). 
[2] See the online documentation for more details on how to attach/detach modules.