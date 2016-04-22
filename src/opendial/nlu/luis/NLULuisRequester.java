package opendial.nlu.luis;

import java.io.IOException;
import java.net.URLEncoder;

import opendial.nlu.NLUAbstractRequester;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * Make HTTP request to Luis
 * @author sstoyanchev
 *
 */
public class NLULuisRequester extends NLUAbstractRequester{

	/**
	 * credentials created by Svetlana using Luis NLU GUI 
	 */
	private static String luisurl = "https://api.projectoxford.ai/luis/v1/application";		
	//private static String genericModelId="fb1cb71a-07c4-4f8b-bb0a-61369e90f4ec";
	private static String subscriptionkey="ee81a5dae9cd44ebbfb8a682acbacd0a";
	
	
	
	 
	public NLULuisRequester(String genericModelId) {
		super(luisurl,genericModelId, NLULuisJson.class);
	}




	@Override
	public String makeRequest(String userUtt) throws IOException{
		OkHttpClient client = new OkHttpClient();
		
		String query = String.format("id=%s&subscription-key=%s&q=%s", 
				 URLEncoder.encode(sessionId, charset),
				 URLEncoder.encode(subscriptionkey, charset),
			     URLEncoder.encode(userUtt, charset));
		Request request = new Request.Builder()
	      .url(url+"?"+query)
	      .build();
		  Response response = client.newCall(request).execute();
		  return response.body().string();	

	}
	


}
