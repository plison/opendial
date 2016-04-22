package opendial.modules.nlu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import opendial.DialogueState;
import opendial.Settings;
import opendial.nlu.NLUAbstractJson;
import opendial.nlu.NLUAbstractRequester;
import opendial.nlu.luis.NLULuisRequester;
import opendial.nlu.mva.NLUmvaRequester;
import opendial.nlu.watson.NLUWatsonRequester;


public class NLUForm {
	
	List<String> fieldTypesList = null;// (List<String>) Arrays.asList("builtin.geography", "builtin.datetime", "builtin.encyclopedia.people");
 	HashMap<String,String> mapFieldTypeToName = new HashMap<String,String>();


	NLUAbstractRequester requester;
	NLUAbstractJson nluJson;
	enum NluType{Luis,Watson,Mva};
	NluType nluType = NluType.Luis; 	
	
	// logger
	public final static Logger log = Logger.getLogger("OpenDial");
	
	/**
	 * create setting name from string and index
	 * to support single and multiple forms
	 */
	String getSetting(Settings settings, String fn, int index) throws Exception{
		
		if (! settings.params.containsKey(fn + "_" + String.valueOf(index)))
		{
			log.info("ERROR: NLUForm setting missing:" +  fn + "_" + String.valueOf(index));
			return "";
		}
		
		return settings.params.getProperty(fn + "_" + String.valueOf(index));
			
	}
	
	
	
	/**
	 * Inintialize the form, given  
	 * @param settings
	 * @param index
	 * @throws Exception 
	 */
	public NLUForm(Settings settings, int index) throws Exception {
		super();
		
		String fieldNames[]  = new String[0];
		String fieldTypes[] = new String[0];
		
        String fieldNamesStr = getSetting(settings,"fieldnames", index); 
        String fieldTypesStr = getSetting(settings,"fieldtypes", index); 
		
		log.info("Form index=" + index + "fieldnameTag = " +fieldNamesStr + "fieldTypesTag = " +fieldTypesStr);
		
		String nlutypeStr = getSetting(settings,"nlu", index);
		
    	if (nlutypeStr.equals("Watson")){
    		String serverStr = getSetting(settings,"server", index); 
    		nluType = NluType.Watson;
    		requester = new NLUWatsonRequester(serverStr);
    	}
    	else if (nlutypeStr.equals("Luis"))
    	{
    		String serverStr = getSetting(settings,"server", index); 
    		nluType = NluType.Luis;
    		String genericModelID = getSetting(settings,"genericModel", index); 
    		requester = new NLULuisRequester(genericModelID);
    	}
    	else if (nlutypeStr.equals("Mva"))
    	{
    		nluType = NluType.Mva;
    		requester = new NLUmvaRequester();
    	}
    	
    	fieldNames  = fieldNamesStr.split(",");
    	fieldTypes  = fieldTypesStr.split(",");
    	fieldTypesList = ((List<String>) Arrays.asList(fieldTypes));
        
        
        Iterator<String> i1 = ((List<String>) Arrays.asList(fieldTypes)).iterator();
        Iterator<String> i2 = ((List<String>) Arrays.asList(fieldNames)).iterator();

        while (i1.hasNext() && i2.hasNext()) {
        	mapFieldTypeToName.put(i1.next(), i2.next());
        }
        if (i1.hasNext() || i2.hasNext()) {
        	log.info("ERROR: NLUForm " + String.valueOf(index) + " field name and field types should be the same length");
        }
        log.info("NLUForm " + String.valueOf(index) + ": Requester type=" + requester.getClass().getName() + "; Field names and types are:" + mapFieldTypeToName.toString()  );

	}
	
	
	/**
	 * Processes NLU
	 * @return
	 */
	public String process(DialogueState state, Collection<String> updatedVars)
	{
		
			log.info("NLUForm.Trigger, u_u; state=" + state.toString());
			log.info("NLUForm.Trigger, u_u; prob=" + state.queryProb("u_u"));
			log.info("NLUForm.Trigger, u_u; best=" + state.queryProb("u_u").getBest().toString());
			String userUtt = state.queryProb("u_u").getBest().toString().toLowerCase();
			
			String contentStr = "";
			try {
				String resp = requester.makeRequest(userUtt);
				log.info("Raw response="+resp.toString());
				NLUAbstractJson nlujson = requester.getObject(resp);
				log.info("Decoded json=" + nlujson.toString());
				//iterate over entities returned by json
				HashMap<String, String> nluresults = nlujson.matchEntities(fieldTypesList);
				System.out.println(nlujson);
				
				List<String> contentEntries = new ArrayList<String>();
				for (Map.Entry<String, String> entry : nluresults.entrySet())
				{
					System.out.println(entry.getKey() + "/" + entry.getValue());
					contentEntries.add(String.format("%s=%s ^ a_u=Set(%s)",mapFieldTypeToName.get(entry.getKey()), 
							entry.getValue(), mapFieldTypeToName.get(entry.getKey())));
				}
				
				//trigger user action a_u in DM
				if (contentEntries.size()==0){
					contentStr = "NotUnderstand=True";
				}
				else
				{
					contentStr = StringUtils.join(contentEntries, "^ ");
				}
				log.info("ContentStr = " + contentStr );

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				contentStr = "a_u=Set(None)";
			}
			
			return contentStr;
	}
	
	
	

}
