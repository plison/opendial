package opendial.nlu.luis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import opendial.nlu.NLUAbstractJson;

public class NLULuisJson extends NLUAbstractJson{
	@Override
	public HashMap<String, String> matchEntities(List<String> entitytype) {
		HashMap<String, String> retMap = new HashMap<String, String>();
		if (entitytype!=null)
			for (String etype: entitytype){
				String value = "";
		        for (NLULuisJson.Entity element : getEntityList()) // or sArray
	            {
	                    if (element.getType().startsWith(etype)){
	                    	value = element.getEntity();
	                    	retMap.put(etype, value);
	                    	
	                    }
	            }
	        
		}
			
		return retMap;
	}
	@Override
	public String toString() {
		return "NLULuisJson [query=" + query + ", inetentList=" + inetents
				+ ", entityList=" + entities + "]";
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public List<Intent> getInetentList() {
		return inetents;
	}
	public void setInetentList(List<Intent> inetentList) {
		this.inetents = inetentList;
	}
	public List<Entity> getEntityList() {
		return entities;
	}
	public void setEntityList(List<Entity> entityList) {
		this.entities = entityList;
	}
	String query;
	List<Intent> inetents;
	List<Entity> entities;
	
	public class Intent{
		public String getIntentTag() {
			return intentTag;
		}
		public void setIntentTag(String intentTag) {
			this.intentTag = intentTag;
		}
		public Double getIntentScore() {
			return intentScore;
		}
		@Override
		public String toString() {
			return "Intent [intentTag=" + intentTag + ", intentScore="
					+ intentScore + "]";
		}
		public void setIntentScore(Double intentScore) {
			this.intentScore = intentScore;
		}
		String intentTag;
		Double intentScore;
	}
	
	public class Entity{

		@Override
		public String toString() {
			return "Entity [entity=" + entity + ", type=" + type
					+ ", startIndex=" + startIndex + ", endIndex=" + endIndex
					+ ", score=" + score + "]";
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
		public Integer getStartIndex() {
			return startIndex;
		}
		public void setStartIndex(Integer startIndex) {
			this.startIndex = startIndex;
		}
		public Integer getEndIndex() {
			return endIndex;
		}
		public void setEndIndex(Integer endIndex) {
			this.endIndex = endIndex;
		}
		public Double getScore() {
			return score;
		}
		public void setScore(Double score) {
			this.score = score;
		}
		String entity;
		String type;
		Integer startIndex;
		Integer endIndex;
		Double score;


	}

}
