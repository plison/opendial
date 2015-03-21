
<p align="center"><img src="https://github.com/plison/opendial/blob/master/resources/opendial-icon.png"></p>

## OpenDial
**Main website**: [http://opendial-toolkit.net](http://opendial-toolkit.net).

**OpenDial** is a Java-based, domain-independent toolkit for developing spoken dialogue systems. The primary focus of OpenDial is on robust and adaptive dialogue management, but OpenDial can also be used to build full-fledged, end-to-end dialogue systems, integrating speech recognition, language understanding, generation and speech synthesis.

The purpose of OpenDial is to combine the benefits of logical and statistical approaches to dialogue modelling into a single framework. The toolkit relies on *probabilistic rules* to represent the internal models of the dialogue domain in a compact and human-readable format. If the probabilistic rules contain unknown parameters, these can be automatically estimated from dialogue data via supervised or reinforcement learning (see [Lison (2014)](http://folk.uio.no/plison/pdfs/thesis/thesis-plison2014.pdf) for details). The hybrid approach adopted by OpenDial makes it possible to learn dialogue models from relatively small amounts of data (dialogue data being often scarce and difficult to transfer across domains) as well as incorporate expert knowledge and domain-specific constraints into the dialogue models.

OpenDial is designed as a blackboard architecture in which all system modules are connected to a central information hub representing the dialogue state (which is encoded as a Bayesian Network). Modules can therefore be plugged in and out of the system without affecting the rest of the processing pipeline. A collection of plugins is available to connect external components (for speech recognition, parsing, speech synthesis, etc.). New modules can also be easily implemented and integrated into the OpenDial architecture.

The toolkit has been originally developed by the [Language Technology Group](http://www.mn.uio.no/ifi/english/research/groups/ltg/) of the University of Oslo (Norway), with [Pierre Lison](http://folk.uio.no/plison) as main developer.
