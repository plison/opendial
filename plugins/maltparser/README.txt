
INSTALLATION
============

This plugin allows you to use the MaltParser (along the Stanford part-of-speech tagger)
to perform grammatical analyses of the user inputs.

To install the plugin, do the following:

1) add the three JAR files liblinear.jar, maltparser.jar and stanford-postagger.jar to 
   the  ./lib directory
2) add the java files MaltParser.java and ParseValue.java to the ./src/opendial/plugins directory
3) recompile OpenDial.
	

START
============

The plugin requires two parameters to be set: one parameter "taggingmodel" that points
to the file path of the POS-tagging model for the Stanford tagger, and one parameter 
"parsingmodel" that points to the Maltparser parsing model.
	
You need to make sure that the parameters mentioned above are set when starting the
system. The easiest way is to add the following to your domain specification:

	<settings>
		<modules>opendial.plugins.MaltParser</modules>
		<taggingmodel>path to the POS-tagging model for Stanford</taggingmodel>
		<parsingmodel>path to the Maltparser model</parsingmodel>
	</settings>

The module is triggered upon each new user input, and outputs a set of alternative parses
in the variable "p_u". The parses are represented as ParseValue objects. Probabilistic rules
can be applied to such ParseValue objects by checking the existence of particular
dependency relations in the parse.  For instance, the following condition will check whether
the parse contains a dependency relation between the head word "move" and the dependent
"to" of label "prep":
	<if var="p_u" value="(move,prep,to)" relation="contains"/>

To be able to perform such checks, the value must a triple where the first element is the
head word, the second element the dependency label, and the third element the dependent word.

