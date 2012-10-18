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
//import HashJsonElement;

public class JsonDocument {

    HashJsonElement root;

    public JsonDocument(String json) throws JsonMalformedException {
        root = new HashJsonElement();
        root.parseJson(json.substring(1, json.length() - 1));
    }
    
    public String toString() {
        return root.toString();
    }
    
    public JsonElement get(String key) {
        return root.get(key);
    }

    public static void main(String[] args) throws JsonMalformedException {
        String document = "{\"type\":\"WIRE_NOTIFY\",\"evType\":\"phrase_result\",\"results\":" +
        		"[{\"activate_lm_time\":\"0.00608587265015\",\"chOpenTime\":\"0.025\",\"clockTime\":" +
        		"\"1.67339897156\",\"cpuTime\":\"0.21\",\"decoded_frame_count\":\"0\",\"frame_count\":" +
        		"\"0\",\"grammar\":\"/n/u205/speechmashups-prod/subfusc/def001/grammars/lm/ASR\",\"hostname\":" +
        		"\"ss-4\",\"majorPageFaults\":\"0\",\"minorPageFaults\":\"9391\",\"norm_score\":\"100\"," +
        		"\"pid\":\"21918\",\"privDirty\":\"17424\",\"reco\":\"hi\",\"result_id\":\"1\",\"session_id\":" +
        		"\"ss-4-201210101223-50DA6\",\"sharedDirty\":\"4600\",\"slot.cost\":19633.2324,\"slot.firstSpeechFrame" +
        		"\":95,\"slot.gdelta\":15,\"slot.glhood\":0,\"slot.gscore\":100,\"slot.hypothesis\":\"hi\"," +
        		"\"slot.lastSpeechFrame\":116,\"slot.likelihood\":0,\"slot.lms\":[\"/n/u205/speechmashups-prod/subfusc/def001/grammars/lm/ASR\"]," +
        		"\"slot.nlu-sisr\":\"greeting\",\"slot.normCost\":129.166,\"slot.normLhood\":0,\"slot.normSpeechLhood\":0," +
        		"\"slot.numFrames\":152,\"slot.numSpeechFrames\":0,\"slot.score\":100,\"slot.udelta\":0,\"slot.ulhood\":0," +
        		"\"slot.uscore\":100,\"speech_start_sample\":\"0\",\"speech_stop_sample\":\"0\",\"trigger\":\"audioEnd\"}]}";
        JsonDocument json = new JsonDocument(document);
        System.out.println(json);
    }
}

