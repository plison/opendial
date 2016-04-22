package opendial.nlu.mva;

import java.io.IOException;
import java.net.URLEncoder;

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
public class NLUmvaRequester extends NLUAbstractRequester {

	private static String mvaurl = "http://interact.2-3-5-7-11.com:8880/Interactful/Hotel/www/hospitality_nlu";
	String ani = "9734527000";
	
	public NLUmvaRequester() {
		super(mvaurl, "", NLUmvaJson.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String makeRequest(String userUtt) throws IOException{

		OkHttpClient client = new OkHttpClient();
		String query = String.format("text=%s&ani=%s", 
				 URLEncoder.encode(userUtt, charset),
				 URLEncoder.encode(ani, charset));
		Request request = new Request.Builder()
	      .url(url+"?"+query)
	      .build();
		  Response response = client.newCall(request).execute();
		  return response.body().string();	
		
	}
	

}
