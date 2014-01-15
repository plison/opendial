// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.domains;


import static org.junit.Assert.*;

import org.junit.Test;

import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Template;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.BasicEffect.EffectType;

public class OutputsTest {

	// logger
	public static Logger log = new Logger("OutputsTest", Logger.Level.NORMAL);

	
	@Test
	public void OutputParsing() {
		
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

