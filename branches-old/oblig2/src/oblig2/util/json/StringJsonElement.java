// =================================================================                                                                   
// Copyright (C) 2011-2013 Sindre Wetjen (sindrewe@ifi.uio.no)                                                                            
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

package oblig2.util.json;
public class StringJsonElement extends JsonElement {
    
    private final JsonType myType = JsonType.STRING;
    private String value;
    
    public void parseJson(String json) {
        value = json;
    }

    public JsonType type() { return myType; }
 
    public String get() {
        return value;
    }

    public String toString() {
        return value;
    }
   
}
