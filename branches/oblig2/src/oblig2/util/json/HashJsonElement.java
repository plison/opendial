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
import java.util.HashMap;
import java.util.LinkedList;

public class HashJsonElement extends JsonElement {

    private final JsonType myType = JsonType.HASH;

    private HashMap<String, JsonElement> elements;

    public HashJsonElement() {
        //System.out.printf("HERE\n");
        elements = new HashMap<String, JsonElement>();
        //System.out.printf("HERE\n");
    }

    public void parseJson(String json) throws JsonMalformedException {
        //System.out.printf("JSON: %s\n", json);
        int toodle = 0;
        int curElmLength = 0;
        char startElm = '"';
        char endElm = '"';
        String currentKey = null;
        char[] j = json.toCharArray();

        for (int x = 0; x < j.length; x++) {

            if (toodle == 0) {
                switch (j[x]) {
                case '[':
                    startElm = '[';
                    endElm = ']';
                    toodle++;
                    curElmLength = 0;
                    break;
                case '{':
                    startElm = '{';
                    endElm = '}';
                    toodle++;
                    curElmLength = 0;
                    break;
                case '"':
                    startElm = '"';
                    endElm = '"';
                    toodle++;
                    curElmLength = 0;
                    break;
                case ':':
                case '\t':
                case ' ':
                case ',':
                    break;
                default:
                    startElm = '1';
                    endElm = ',';
                    curElmLength = 0;
                    toodle++;
                }
                
            } else {
                if (j[x] == endElm && (j[x] - 1 != '\\' || endElm != startElm)) {
                    toodle--;
                    
                    if (toodle == 0 && currentKey != null) {
                        //System.out.printf("%d - %d\n", x, curElmLength);
                        if (endElm == ',') {
                            elements.put(currentKey, 
                                         newJsonElement(startElm, 
                                                        json.substring(x - curElmLength - 1, x)));
                        } else {
                            elements.put(currentKey, 
                                         newJsonElement(startElm, 
                                                        json.substring(x - curElmLength, x)));
                        }
                        currentKey = null;
                    } else if (toodle == 0) {
                        currentKey = json.substring(x - curElmLength, x);
                        //System.out.printf("%s : %d : %d\n", currentKey, x, curElmLength);     
                    }
                } else if (j[x] == startElm) toodle++;
                curElmLength++;
            }
            if (toodle < 0 || curElmLength < 0){
                //System.out.printf("%d :: %d :: %d :: %d\n", x, toodle, curElmLength);
                throw new JsonMalformedException();
            }
        }
    }

    public JsonType type() { return myType; }

    public JsonElement get(String key) {
        return elements.get(key);
    }

    public String toString() {
        String r = "{";
        for (String key:elements.keySet()) {
            r += key + ":" + elements.get(key) + ",";
        }
        return r.substring(0, r.length() - 1) + "}";
    }
}
