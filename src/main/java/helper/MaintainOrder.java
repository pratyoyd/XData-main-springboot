/**
 * 
 */
package helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONObject;



import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;


public class MaintainOrder {


	/**
	 * This method will change the HashMap to LinkedHashMap 
	 * as the backend implementation of the JSONObject
	 * @param jsonObject: The Input JSONObject to be formatted
	 */
	private static void sortJSONObj(JSONObject jsonObject) {
		try {
			Field changeMap = JSONObject.class.getDeclaredField("map");
			changeMap.setAccessible(true);
			changeMap.set(jsonObject, new LinkedHashMap<>());
			changeMap.setAccessible(false);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will take the a json file and format it 
	 * @param outFile: Original File
	 * @param formattedOutFile: Formatted file
	 */
	public static JSONObject formatJson(org.json.simple.JSONObject outJSON, ArrayList<String> keyset)
	{

		JSONObject formattedOutputJson = new JSONObject();
		
		formattedOutputJson.put("mutantDetected", outJSON.get("mutantDetected"));

		//		Inserting the Array of Mutant Databases

		String mutdbsStr = (String) outJSON.get("mutantDatasets").toString();
		JSONArray mutdbs = new JSONArray(mutdbsStr);
		JSONArray outMutDbs = new JSONArray();
		if(mutdbs.length() > 0)
		{
			for(int loop =0; loop < mutdbs.length() ; loop++)
			{
				JSONObject objDb = (JSONObject) mutdbs.get(loop);
				JSONObject objOutMut = new JSONObject();

				MaintainOrder.sortJSONObj(objOutMut);

				for(String key : keyset) 
					objOutMut.put(key, objDb.get(key));
				outMutDbs.put(objOutMut);
			}
		}

		formattedOutputJson.put("mutantDatasets", outMutDbs);


		//		 Inserting final exceptions field
		formattedOutputJson.put("exceptions", outJSON.get("exceptions"));

		//System.out.println(formattedOutputJson.toString(4));

		//        Persisting in file
		
		return formattedOutputJson;
	}

	/**
	 * @param args
	 * @throws SecurityException 
	 * @throws IOException 
	 */
	public static void main(String[] args)  
	{
		String outFile = "/Users/manish/eclipse-workspace/executable/sample_json/apioutput.json";
		String formattedOutFile = "/Users/manish/eclipse-workspace/executable/sample_json/apioutput_formatted.json";

		//		Defining Order of JSON Fields
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.add("mutantDBType");
		keyset.add("mutantDBContent");
		keyset.add("rsExtracted(Instructor)");
		keyset.add("rsHidden(Student)");

		//MaintainOrder.formatJson(outFile, formattedOutFile,keyset);
	}

}
