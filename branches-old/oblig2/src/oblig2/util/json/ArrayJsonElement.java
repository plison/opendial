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
import java.util.LinkedList;

public class ArrayJsonElement extends JsonElement {

    private final JsonType myType = JsonType.ARRAY;

    private LinkedList<JsonElement> elements;

    public ArrayJsonElement() {
        elements = new LinkedList<JsonElement>();
    }

    public void parseJson(String json) throws JsonMalformedException {
        int toodle = 0;
        int curElmLength = 0;
        char startElm = '"';
        char endElm = '"';
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
                case '\t':
                case ' ':
                case ',':
                    break;
                default:
                    startElm = '1';
                    endElm = ',';
                    curElmLength = -1;
                    toodle++;
                }
                    
            } else {

                if (j[x] == endElm && (j[x] - 1 != '\\' || endElm != startElm)) {
                    toodle--;
                    
                    if (toodle == 0) {
                        //System.out.printf("%d - %d :: %c \n", x, curElmLength, j[x]);
                        if (endElm == ',') {
                            elements.add(newJsonElement(startElm, json.substring(x - curElmLength, x - 1)));
                        } else {
                            elements.add(newJsonElement(startElm, json.substring(x - curElmLength, x)));
                        }
                    }
                    
                }
                curElmLength++;
            }
            if (toodle < 0 || curElmLength <  -1) throw new JsonMalformedException();
        }
    }

    public JsonType type() { return myType; }
    
    public JsonElement get(int x) {
        return elements.get(x);
    }

    public LinkedList<JsonElement> getList() {
        return elements;
    }

    public String toString() {
        String r = "[";
        for (JsonElement e:elements) {
            r += e + ",";
        }
        return r.substring(0, r.length() - 1) + "]";
    }
}
