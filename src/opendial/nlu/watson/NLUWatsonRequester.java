package opendial.nlu.watson;

import java.io.IOException;

import opendial.nlu.NLUAbstractRequester;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

/**
 * Requester for watson http
 * @author sstoyanchev
 *
 */
public class NLUWatsonRequester extends NLUAbstractRequester {

		public NLUWatsonRequester(String watsonurl) {
		super(watsonurl, "", NLUWatsonJson.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String makeRequest(String userUtt) throws IOException{
		
		OkHttpClient client = new OkHttpClient();
		
		RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), userUtt);
		
		Request request = new Request.Builder()
		  .addHeader("Content-Type", "text/plain")
	      .url(url)
	      .post(body)
	      .build();
		
		
		Response response = client.newCall(request).execute();
		return response.body().string();	
	 
	}
	
	
	
	


}
