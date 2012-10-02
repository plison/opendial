
from oblig2 import *
from oblig2.actions import *


class BasicPolicy(DialoguePolicy):
	"""Example of basic policy for the dialogue system"""
	
	# every policy must implement this method: it receives an N-Best list
	# and must then process it and find the appropriate response,
	# in the form of an Action (in this case, a DialogueAction)
	def processInput(self, nbest, dialstate, worldstate):
		print "input is " + str(nbest)
		action = DialogueAction("you said " + nbest.getHypotheses()[0].getString())
		return action


# AT&T parameters
uuid = "F9A9D13BC9A811E1939C95CDF95052CC"
appname = "def001"
grammar = "numbers"


# should be changed to your own policy!
policy = BasicPolicy()
DialogueSystem(policy, ConfigParameters(uuid, appname, grammar));
