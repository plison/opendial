package opendial.nlu.mva;

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
public class NLUmvaJson extends NLUAbstractJson{

	
	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public HashMap<String, String> matchEntities(List<String> entitytype) {
		HashMap<String, String> retMap = new HashMap<String, String>();
		if (entitytype!=null)
			for (String etype: entitytype){
				String value = "";
		        for (NLUmvaJson.Tag element : getTags()) // or sArray
	            {
	                    if (element.getAttr().startsWith(etype)){
	                    	value = element.getVal();
	                    	retMap.put(etype, value);
	                    	
	                    }
	            }
	        
		}
			
		return retMap;

	}


	@Override
	public String toString() {
		return "NLUmvaJson [intent=" + intent + ", response=" + response
				+ ", tags=" + tags + "]";
	}


	String intent;
	String response;
	List<Tag> tags;
	class Tag{
		@Override
		public String toString() {
			return "Tag [attr=" + attr + ", val=" + val + "]";
		}
		public String getAttr() {
			return attr;
		}
		public void setAttr(String attr) {
			this.attr = attr;
		}
		public String getVal() {
			return val;
		}
		public void setVal(String val) {
			this.val = val;
		}
		String attr;
		String val;
	}
	   
   }
   
   

