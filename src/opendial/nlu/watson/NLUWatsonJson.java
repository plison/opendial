package opendial.nlu.watson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import opendial.nlu.NLUAbstractJson;
import opendial.nlu.luis.NLULuisJson;

import com.google.gson.annotations.SerializedName;

/**
 * This class mirrors watson's json output that is relevant for NLU
 * (OutAPINLU portion)
 * @author sstoyanchev
 *
 */
public class NLUWatsonJson extends NLUAbstractJson{

	
	@Override
	public HashMap<String, String> matchEntities(List<String> entitytype) {
		HashMap<String, String> retMap = new HashMap<String, String>();
		if (entitytype!=null)
			for (String etype: entitytype){
				String value = "";
				//iterate over segments
				if (getSeglist()!=null)
				for (int segindex=0; segindex< getSeglist().size(); segindex++)
				{
			        for (NLUWatsonJson.Segment.OutAPI.Entity element : getSeglist().get(segindex).getOutAPI().getEntities()) // or sArray
		            {
		                    if (element.getType().startsWith(etype)){
		                    	value = element.getNorm();
		                    	if (value==null || value.equals("")){
		                    		value = element.getEntity();
		                    	}
		                    	retMap.put(etype, value);
		                    	
		                    }
		            }
				}
		        
			}
			
		return retMap;
	}

@Override
	public String toString() {
		return "NLUWatsonJson [seglist=" + segList + ", rawtext=" + rawText
				+ ", hostname=" + hostname + "]";
	}

public List<Segment> getSeglist() {
		return segList;
	}


	public void setSeglist(List<Segment> seglist) {
		this.segList = seglist;
	}


	public String getRawtext() {
		return rawText;
	}


	public void setRawtext(String rawtext) {
		this.rawText = rawtext;
	}


	@SerializedName("seg-list")  List<Segment> segList;
   
   public String getHostname() {
	   return hostname;
   }


	public void setHostname(String hostname) {
		this.hostname = hostname;
	}


   @SerializedName("raw-text") String rawText;
   String hostname;
   
   
   /**
    * Corresponds to each sentence segment of the output
    * @author sstoyanchev
    *
    */
   public class Segment{
	   
	   @Override
	public String toString() {
		return "Segment [outAPI=" + outAPI + "]";
	}

	public OutAPI getOutAPI() {
		return outAPI;
	}

	public void setOutAPI(OutAPI outAPI) {
		this.outAPI = outAPI;
	}

	@SerializedName("NLUHotel") OutAPI outAPI;
	   
	   public class OutAPI{
		   @Override
		public String toString() {
			return "OutAPI [entities=" + entities + "]";
		}

		public List<Entity> getEntities() {
			return entities;
		}

		public void setEntities(List<Entity> entities) {
			this.entities = entities;
		}

		List<Entity> entities;
		   
		   //this is the final Entity class
		   public class Entity{
			   @Override
			public String toString() {
				return "Entity [confidence=" + confidence + ", entity="
						+ entity + ", norm="
								+ norm + ", type=" + type + ", wordindexes="
						+ wordindexes + "]";
			}
			public Integer getConfidence() {
				return confidence;
			}
			public void setConfidence(Integer confidence) {
				this.confidence = confidence;
			}
			public String getEntity() {
				return entity;
			}
			public void setEntity(String entity) {
				this.entity = entity;
			}
			public String getType() {
				return type;
			}
			public void setType(String type) {
				this.type = type;
			}
			public List<Integer> getWordindexes() {
				return wordindexes;
			}
			public void setWordindexes(List<Integer> wordindexes) {
				this.wordindexes = wordindexes;
			}
			public String getNorm() {
				return norm;
			}
			public void setNorm(String norm) {
				this.norm = norm;
			}
			
			   Integer confidence;
			   String entity;
			   String type;
			   String norm;

			   List<Integer> wordindexes;
		   }
		   }
	   }
	   
   }
   
   

