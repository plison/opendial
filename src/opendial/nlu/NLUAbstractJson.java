package opendial.nlu;

import java.util.List;
import java.util.HashMap;
import java.util.List;

public abstract class NLUAbstractJson {
	/**
	 * returns a hash map with keys in entity type and values from nlu result
	 * @param entitytype
	 * @return
	 */
	abstract public HashMap<String,String > matchEntities(List<String> entitytype);

}
