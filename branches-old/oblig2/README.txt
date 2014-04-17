
====================================
  	INF5820 / INF9820 
 	OBLIGATORY ASSIGNMENT 2
 ====================================

1) Requirements
---------------

You need to have a recent version of Java installed, as well as Ant (http://ant.apache.org/)
for the compilation process.

If you prefer to program in Python instead of Java, you also need to install Jython.  
In this case, you MUST specify the home directory of your Jython installation:

	export JYTHON_HOME=/your/directory/where/Jython/is/installed
	
In order to run the dialogue system, you also need to:
	- have a microphone connected (preferably a headset microphone to get decent speech 
	  recognition results, but a built-in microphone should also work)
	- have a working Internet connection (in order to connect to the AT&T servers for 
	  ASR and TTS)
	

2) Compilation
---------------

The easiest way to compile the code for the assignment is via Ant.  Simply type:

	ant compile
	
and the code will automatically be compiled.

Note that you also have to compile the Java code if you use Jython!


3) Use
---------------

To run the dialogue system, you can also use ant:

	ant run
	
To run the Jython code, you can use the following command:

	ant run_jython
	
	(it will only work if you have specified JYTHON_HOME; cf. above)
	

In both cases, don't forget to change the uuid, appname and grammar to your own 
parameters (see the code in Main.java or Main.py)!

The Main scripts are located in test/oblig2.

4) Contact
---------------

Please don't hesitate to contact me if you experience any problems, or have some
questions to ask:
	plison@ifi.uio.no
	

