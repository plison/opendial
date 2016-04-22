package opendial.nlu;

import java.io.IOException;

import com.google.gson.Gson;

/**
 * derive requester classes for each vendor from this class
 * @author sstoyanchev
 *
 */
public abstract class NLUAbstractRequester {
	
	protected String charset = "UTF-8";

	protected String url;
	protected String sessionId;
	protected Class jsonClass;

	
	public NLUAbstractRequester(String url, String sessionId, Class jsonClass) {
		super();
		this.url = url;
		this.sessionId = sessionId;
		this.jsonClass = jsonClass;
	}

	abstract public String makeRequest(String utt) throws IOException;
	
	
	public String getInfo(){
		return url + " " + sessionId;
	}
	
	public NLUAbstractJson getObject(String str) throws IOException{
		Gson gson = new Gson();

			//convert the json string back to object
		NLUAbstractJson obj = (NLUAbstractJson) gson.fromJson(str, jsonClass);

		return obj;
	}

}
