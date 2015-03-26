// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains;


import static org.junit.Assert.assertEquals;
import opendial.arch.Logger;
import opendial.datastructs.Template;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;

import org.junit.Test;

public class OutputsTest {

	// logger
	public static Logger log = new Logger("OutputsTest", Logger.Level.NORMAL);

	
	@Test
	public void testOutputs() {
		
		Effect o = new Effect();
		assertEquals(o, Effect.parseEffect("Void"));
		o.addSubEffect(new BasicEffect(new Template("v1"), new Template("val1"), EffectType.SET));
		assertEquals(o, Effect.parseEffect("v1:=val1"));

		o.addSubEffect(new BasicEffect(new Template("v2"), new Template("val2"), EffectType.ADD));
		assertEquals(o, Effect.parseEffect("v1:=val1 ^ v2+=val2"));
		
		o.addSubEffect(new BasicEffect(new Template("v2"), new Template("val3"), EffectType.DISCARD));
		assertEquals(o, Effect.parseEffect("v1:=val1 ^ v2+=val2 ^ v2!=val3"));
		
		o.addSubEffect(new BasicEffect(new Template("v3"), null, EffectType.CLEAR));
		assertEquals(o, Effect.parseEffect("v1:=val1 ^ v2+=val2 ^ v2!=val3 ^ v3:={}"));
		
	}
}

