package test;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConstants.Field;

import helper.ExecuteJar;
import helper.MaintainOrder;
import testDataGen.GenerateDataSet;
import testDataGen.PopulateTestData;
import util.Configuration;
import util.TableMap;
import util.Utilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.ibatis.jdbc.ScriptRunner;

public class RegressionTests {

	String basePath;
	String schema;
	String sampleData;
	private static String ip;
	private static String port;
	public  static String databaseUser;
	public static String databasePassword;
	public static String databaseName;
	

	public RegressionTests(String basePath, String schemaFile, String sampleDataFile) {
		super();
		this.basePath = basePath;
		this.schema = Utilities.readFile(new File(schemaFile));
		this.sampleData = Utilities.readFile(new File(sampleDataFile));
	}
	
	public RegressionTests(String schema, String sampleData) {
		super();		
		this.schema = schema;
		this.sampleData = sampleData;
		
		
	}

	public static Connection getTestConn() throws Exception{
		Class.forName("org.postgresql.Driver");
		String loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
		return DriverManager.getConnection(loginUrl, Configuration.getProperty("existingDatabaseUser"), Configuration.getProperty("existingDatabaseUserPasswd"));
	}
	public static Connection getTestConn(String ip,String port, String databaseName,String databaseUser, String databasePassword) throws Exception{
		Class.forName("org.postgresql.Driver");
		
		String loginUrl = "jdbc:postgresql://" + ip + ":" + port + "/" + databaseName;
		System.out.println(loginUrl);
		return DriverManager.getConnection(loginUrl, databaseUser, databasePassword);
		
	}

	/**
	 * Load queries from the queries.txt file
	 * @return Map of queryId,query
	 * @throws IOException
	 */
	public Map<Integer,String> getQueries()	throws IOException {
		Map<Integer,String> queryMap=new HashMap<Integer,String>();
		
		String fullPath=basePath+File.separator+"queries_Q3.txt";
		
		FileReader fileReader = new FileReader(new File(fullPath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if(line.trim().startsWith("--"))
				continue;
			//The queries file contains entries for queries in the format id|query
			String[] lineArr=line.split("\\|", 2);
			if(lineArr.length<2)
				continue;
			if(lineArr[1]==null || lineArr[1].trim().equals(""))
				continue;
			Integer queryId=0;
			try {
				queryId=Integer.parseInt(lineArr[0].trim());
			} catch(NumberFormatException nfe) {
				continue;
			}
			queryMap.put(queryId, lineArr[1]);
		}
		fileReader.close();
		
		
		return queryMap;
	}
	
	/**
	 * Gets mutants from the mutants.txt file
	 * @return map of queryId, list of mutants
	 * @throws IOException
	 */
	public Map<Integer,List<String>> getMutants() throws IOException	{
		Map<Integer,List<String>> mutantMap=new HashMap<Integer,List<String>>();
		
		String fullPath=basePath+File.separator+"mutants_Q3_min.txt";
		
		FileReader fileReader = new FileReader(new File(fullPath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if(line.trim().startsWith("--"))
				continue;
			//The mutants file contains entries for queries in the format id|query. The id should match the query
			String[] lineArr=line.split("\\|", 2);
			if(lineArr.length<2)
				continue;
			if(lineArr[1]==null || lineArr[1].trim().equals(""))
				continue;
			Integer queryId=0;
			try {
				queryId=Integer.parseInt(lineArr[0].trim());
			} catch(NumberFormatException nfe) {
				continue;
			}
			List<String> mutantList= mutantMap.get(queryId);
			if(mutantList==null)	{
				mutantList=new ArrayList<String>();
			}
			mutantList.add(lineArr[1]);
			mutantMap.put(queryId, mutantList);
		}
		fileReader.close();
		
		
		return mutantMap;
	}
	
	
	private List<String> generateDataSets(Integer queryId, String query)	{
		
		try(Connection conn =getTestConn(ip, port, databaseName, databaseUser, databasePassword)){

			boolean orderDependent=false;
			String tempFilePath=File.separator+queryId;
			GenerateDataSet d=new GenerateDataSet();
			List<String> datasets=d.generateDatasetForQuery(conn,queryId,query,  schema,  sampleData,  orderDependent,  tempFilePath, null);
			System.out.println(datasets);
			return datasets;
		} catch(Exception e) {
			e.printStackTrace();
		}

		return null;
		
	}
	
	/**
	 * Tests if the basic dataset produces a non-empty result
	 * @param queryId queryId of the dataset
	 * @param query query
	 * @return
	 */
	public boolean testBasicDataset(Integer queryId, String query)	{
		
		try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
			String filePath=queryId+"";
			
			PopulateTestData.deleteAllTablesFromTestUser(testConn);
			GenerateDataSet.loadSchema(testConn, schema);
			//GenerateDataSet.loadSampleData(testConn, sampleData);
			
			TableMap tableMap=TableMap.getInstances(testConn, 1);
			
			PopulateTestData.loadCopyFileToDataBase(testConn, "DS0", filePath, tableMap);
			
			
			PreparedStatement ptsmt=testConn.prepareStatement(query);
			ResultSet rs=ptsmt.executeQuery();
			
		
			if (!rs.isBeforeFirst() ) {    
			    System.out.println(" Result from test basic dataset: No data"); 
			} 
			
				
				while (rs.next()) {
					
					//System.out.println("HELLO");
					ResultSetMetaData rsmd = rs.getMetaData();
					 int columnsNumber = rsmd.getColumnCount();				
				       for (int i = 1; i <= columnsNumber; i++) {
				           if (i > 1) System.out.print(",  ");
				           String columnValue = rs.getString(i);
				           System.out.print(columnValue + " " + rsmd.getColumnName(i));
				       }
				       System.out.println("");
				return true;
			}
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean testMutantKilling(Integer queryId, List<String> datasets, String query, String mutant) {
		
		for(String datasetId:datasets) {
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				
				
				GenerateDataSet.loadSchema(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);

				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					return true;
				}
				
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				return true;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
public List<String> testEachMutant(Integer queryId, List<String> datasets, String query, String mutant) {
		List<String> datasetList = new ArrayList<String>();
		for(String datasetId:datasets) {
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				String dsPath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
				
				GenerateDataSet.loadSchema(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);

				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					String datasetNo = datasetId.substring(2);
					String datasetFilePath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+"cvc3_"+datasetNo+".cvc";
					File file = new File(datasetFilePath);
					Scanner scanner = new Scanner(file);
					while (scanner.hasNextLine()) {
						   String lineFromFile = scanner.nextLine();
						   if(lineFromFile.contains("MUTATION TYPE")) { 
							String DatasetTypeIndicator = lineFromFile.replace("%MUTATION TYPE: ", "");
					datasetList.add(DatasetTypeIndicator);
					System.out.println(DatasetTypeIndicator);
				
				}
					}
				}
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				List<String> error = new ArrayList<String>(); 
				error.add("Errors while killing mutation");
				return error;
				
				
				
			} catch(Exception e) {
				e.printStackTrace();
				e.printStackTrace();
				List<String> error = new ArrayList<String>(); 
				error.add("Errors while killing mutation");
				return error;
				
			}
		}
		
		return datasetList;
	}

	public List<List<String>> allMutantDatabases(Integer queryId, List<String> datasets, String query, String mutant){
		List<List<String>> mutantdbs = new ArrayList<>();
		for(String datasetId:datasets) {
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				String dsPath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
				
				GenerateDataSet.loadSchema(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);

				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println(testQuery);
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					String datasetNo = datasetId.substring(2);
					String datasetFilePath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
					ArrayList<String> copyFileList=new ArrayList<String>();
					ArrayList <String> copyFilesWithFk = new ArrayList<String>();
					Pattern pattern = Pattern.compile("^DS([0-9]+)$");
					java.util.regex.Matcher matcher = pattern.matcher(datasetId);
					
					File ds=new File(dsPath);
					String copyFiles[] = ds.list();
					String st ="";
					for(int j=0;j<copyFiles.length;j++){
						//	if(copyFiles[j].contains(".ref")){
						//	copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".ref")));
						//}else{

						copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".copy")));
						//}
					}
					List<String> dbvaluesforCurrentMutation = new ArrayList<String>();
					for(int j=0;j<copyFiles.length;j++){

						String copyFileName = copyFiles[j];
						
							//Check for primary keys constraint and add the data to avoid duplicates


							String tname =copyFileName.substring(0,copyFileName.indexOf(".copy"));

							BufferedReader br = new BufferedReader(new FileReader(dsPath+"/"+copyFileName));
							String row ="";
							while((st=br.readLine())!=null){

								row="'"+tname+"','"+ st.replaceAll("\\|", "','")+"'";
								
				}
							dbvaluesforCurrentMutation.add(row);
				}
					mutantdbs.add(dbvaluesforCurrentMutation);
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}
				
			}catch(SQLException e) {
				e.printStackTrace();
				
				
				
				
			} catch(Exception e) {
				e.printStackTrace();
				
				
			}
		}
		
		return mutantdbs;
	}
	

	
	public boolean testLimitMutants(String query, String mutant)
	{
		
		try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
			int countQuery =0, countMutant =0;
			PopulateTestData.deleteAllTablesFromTestUser(testConn);
			GenerateDataSet.loadSchema(testConn, schema);
			GenerateDataSet.loadSampleData(testConn, sampleData);
			String countOfQuery = "select count(1) from (" + query + ") as count";
			//System.out.println(countOfQuery);
			PreparedStatement ptsmt=testConn.prepareStatement(countOfQuery);
			ResultSet rs=ptsmt.executeQuery();
			while(rs.next()) {
				countQuery  = rs.getInt("count");
			}
			String countOfMutant = "select count(1) from (" + mutant + ") as count";
			PreparedStatement ptsmt1 = testConn.prepareStatement(countOfMutant);
			ResultSet rs1 = ptsmt1.executeQuery();
			while(rs1.next()) {
				countMutant  = rs1.getInt("count");
			}
			if(countQuery == countMutant)return true;
			
			
	}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public Map<Integer,List<String>> runRegressionTests(){
		
		Map<Integer,String> queryMap;
		Map<Integer,List<String>> mutantMap;
		
		Map<Integer,List<String>> testResult=new LinkedHashMap<Integer,List<String>>();
		
		try {	
			queryMap = getQueries();
			mutantMap = getMutants();
		}catch(Exception e) {
			System.out.println("Error reading queries or mutants");
			e.printStackTrace();
			return null;
		}
		
		for(Integer queryId:queryMap.keySet()) {
			List<String> errors=new ArrayList<String>();
			
			String query=queryMap.get(queryId);
			List<String> datasets;
			//Generate datasets
			datasets=generateDataSets(queryId,query);
			
			if(datasets==null || datasets.isEmpty()) {
				errors.add("Exception in generating datasets");
				testResult.put(queryId, errors);
				continue;
			}
				
			//Check if DS0 works
			try {
				if(testBasicDataset(queryId,query)==false)
					errors.add("Basic datasets failed");
			} catch (Exception e)	{
				e.printStackTrace();
				errors.add("Exception in running query on basic test case");
				testResult.put(queryId, errors);
				continue;
			}
			
			
			//Check mutation killing
			for(String mutant:mutantMap.get(queryId))	{
				try {		
					
					if(testMutantKilling(queryId, datasets, query, mutant)==true)
						errors.add("Query Mutant found: "+mutant);
					else {
					if(query.contains(" LIMIT ") || query.contains(" limit "))
					if(testLimitMutants(query, mutant)==false)errors.add("Limit mutant found");
					}
				}catch (Exception e)	{
					e.printStackTrace();
					errors.add("Exception in killing mutant query:"+mutant);
					testResult.put(queryId, errors);
				}
			}
			
			
			
			
			
			
			if(!errors.isEmpty())
				testResult.put(queryId, errors);
			
		}
		
		return testResult;
	}
	
	public  JSONObject returnXDataResult(String testQuery, String schema, String sampleData, String executablePath, String executableCommand, String fileName, String executableTableName  ) throws IOException 
	{
		
		String mutantQuery = "Select * from student_result";
		Map<Integer,String> queryMap = new HashMap<Integer,String>() ;
		Map<Integer,List<String>> mutantMap=new HashMap<Integer,List<String>>();
		List<String> mutants = new ArrayList<String>();
		List<String> datasetList = new ArrayList<String>();
		List<List<String>> mutantDBContent = new ArrayList<>();
		Map<Integer,List<List<String>>>  resultList = new HashMap<>();
		String result ="";
		mutants.add(mutantQuery);
		queryMap.put(1, testQuery);
		mutantMap.put(1, mutants);
		
		List<String> errors=new ArrayList<String>();
		String exception ="";
		for(Integer queryId:queryMap.keySet()) {
			
			
			String query=queryMap.get(queryId);
			List<String> datasets;
			//Generate datasets
			datasets=generateDataSets(queryId,query);
			
			if(datasets==null || datasets.isEmpty()) {
				errors.add("Exception in generating datasets");
				exception = "Exception in generating datasets";
				continue;
			}
				
			//Check if DS0 works
			try {
				if(testBasicDataset(queryId,query)==false)
					errors.add("Basic datasets failed");
			} catch (Exception e)	{
				e.printStackTrace();
				errors.add("Exception in running query on basic test case");
				exception = "Exception in running query on basic test case";
				continue;
			}
			System.out.println("Error size after basic dataset: " + errors.size());
			
			//Check mutation killing
			for(String mutant:mutantMap.get(queryId))	{
				try {
					if(testMutantKilling(queryId, datasets, query,executablePath, executableCommand, fileName,executableTableName )== true)
					{
						errors.add(mutant);
					datasetList= testEachMutant(queryId, datasets, query,executablePath, executableCommand, fileName, executableTableName);
					mutantDBContent = allMutantDatabases(queryId,datasets,query,executablePath, executableCommand, fileName, executableTableName);
					resultList =FindResultSetForMutants(queryId,datasets,query,executablePath,executableCommand,fileName,executableTableName );
					}
					
					
				}catch (Exception e)	{
					e.printStackTrace();
					errors.add("Exception in killing mutant query:"+ mutant);
					exception ="Exception in killing mutant query";
				}
			}
			System.out.println("Error size after mutant killing: " + errors.size());
			if(errors.isEmpty()) {
				result="All Test Cases Passed";
			} else {
				result+="Following Test Cases Failed";
				for(Integer i=0;i<errors.size();i++) {
					result+=errors.get(i);
				}
			}
			
			
			
		}		
		
		JSONObject jsonObject = new JSONObject();
		if(!exception.equals("") || datasetList.contains("Errors while killing mutation")){
			jsonObject.put("mutantDetected","not applicable");
			JSONArray array = new JSONArray();			
			
			jsonObject.put("mutantDatasets", array);
			if(exception == "")exception = "Errors while killing mutation";
			jsonObject.put("exceptions", exception);
			
	}
	else {
		if(errors.isEmpty()) {
			jsonObject.put("mutantDetected","no");
			JSONArray array = new JSONArray();
			for (int i = 0; i < datasetList.size(); i++) {
				JSONObject mutantdbdetails = new JSONObject();
				mutantdbdetails.put("mutantDBType",datasetList.get(i));
				mutantdbdetails.put("mutantDBContent", mutantDBContent.get(i));
				mutantdbdetails.put("rsExtracted(Instructor)", "");
				jsonObject.put("rsHidden(Student)", "");
				array.add(mutantdbdetails);
			}
			
			jsonObject.put("mutantDatasets", array);
			
			
			jsonObject.put("exceptions", exception);
			
		
	}
		else {
			jsonObject.put("mutantDetected","yes");
			JSONArray array = new JSONArray();
			for (int i = 0; i < datasetList.size(); i++) {
				JSONObject mutantdbdetails = new JSONObject();
				mutantdbdetails.put("mutantDBType",datasetList.get(i));
				mutantdbdetails.put("mutantDBContent", mutantDBContent.get(i));
				mutantdbdetails.put("rsExtracted(Instructor)", resultList.get(i).get(0));
				mutantdbdetails.put("rsHidden(Student)", resultList.get(i).get(1));
				array.add(mutantdbdetails);
			}
			
			jsonObject.put("mutantDatasets", array);
			
			
			
			
			jsonObject.put("exceptions", exception);
			
		}
	
	//return result;
	}
		return jsonObject;
	}	
	private List<List<String>> allMutantDatabases(Integer queryId, List<String> datasets, String query, String executablePath,String executableCommand, String fileName,String executableTableName ) {
		List<List<String>> mutantdbs = new ArrayList<>();
		for(String datasetId:datasets) {
			
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				
				String mutant = "select * from student_result";
				if(!executableTableName.equals(""))mutant = "select * from "+executableTableName;
				String dsPath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
				
				GenerateDataSet.loadSchema_NotTemp(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				String runArgs = executableCommand;
				String jarFile = executablePath + fileName;
				ExecuteJar.executeJar(jarFile,runArgs);
				
				

				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					String datasetNo = datasetId.substring(2);
					String datasetFilePath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
					ArrayList<String> copyFileList=new ArrayList<String>();
					ArrayList <String> copyFilesWithFk = new ArrayList<String>();
					Pattern pattern = Pattern.compile("^DS([0-9]+)$");
					java.util.regex.Matcher matcher = pattern.matcher(datasetId);
					
					File ds=new File(dsPath);
					String copyFiles[] = ds.list();
					String st ="";
					for(int j=0;j<copyFiles.length;j++){
						//	if(copyFiles[j].contains(".ref")){
						//	copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".ref")));
						//}else{

						copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".copy")));
						//}
					}
					List<String> dbvaluesforCurrentMutation = new ArrayList<String>();
					for(int j=0;j<copyFiles.length;j++){

						String copyFileName = copyFiles[j];
						
							//Check for primary keys constraint and add the data to avoid duplicates


							String tname =copyFileName.substring(0,copyFileName.indexOf(".copy"));

							BufferedReader br = new BufferedReader(new FileReader(dsPath+"/"+copyFileName));
							String row ="";
							while((st=br.readLine())!=null){

								row="'"+tname+"','"+ st.replaceAll("\\|", "','")+"'";
								
				}
							dbvaluesforCurrentMutation.add(row);
				}
					mutantdbs.add(dbvaluesforCurrentMutation);
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}
				
			}catch(SQLException e) {
				e.printStackTrace();
				
				
				
				
			} catch(Exception e) {
				e.printStackTrace();
				
				
			}
		}
		
		return mutantdbs;
	}

	private List<String> testEachMutant(Integer queryId, List<String> datasets, String query, String executablePath,String executableCommand, String fileName, String executableTableName ) {
		List<String> datasetList = new ArrayList<String>();
		for(String datasetId:datasets) {
			
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				String mutant = "select * from student_result";
				if(!executableTableName.equals(""))mutant = "select * from "+executableTableName;
				String dsPath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+datasetId;
				
				GenerateDataSet.loadSchema_NotTemp(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				String runArgs = executableCommand;
				String jarFile = executablePath + fileName;
				ExecuteJar.executeJar(jarFile,runArgs);

				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					String datasetNo = datasetId.substring(2);
					String datasetFilePath = Configuration.homeDir+"/temp_cvc"+File.separator+filePath+File.separator+"cvc3_"+datasetNo+".cvc";
					File file = new File(datasetFilePath);
					Scanner scanner = new Scanner(file);
					while (scanner.hasNextLine()) {
						   String lineFromFile = scanner.nextLine();
						   if(lineFromFile.contains("MUTATION TYPE")) { 
							String DatasetTypeIndicator = lineFromFile.replace("%MUTATION TYPE: ", "");
					datasetList.add(DatasetTypeIndicator);
					System.out.println(DatasetTypeIndicator);
				
				}
					}
				}
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				List<String> error = new ArrayList<String>(); 
				error.add("Errors while killing mutation");
				return error;
				
				
				
			} catch(Exception e) {
				e.printStackTrace();
				e.printStackTrace();
				List<String> error = new ArrayList<String>(); 
				error.add("Errors while killing mutation");
				return error;
				
			}
		}
		
		return datasetList;
	}

	private boolean testMutantKilling(Integer queryId, List<String> datasets, String query, String executablePath,String executableCommand, String fileName,  String executableTableName  ) {
		for(String datasetId:datasets) {
			
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				String mutant = "select * from student_result";
				if(!executableTableName.equals(""))mutant = "select * from "+executableTableName;
				
				GenerateDataSet.loadSchema_NotTemp(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				String runArgs = executableCommand;
				String jarFile = executablePath + fileName;
				ExecuteJar.executeJar(jarFile,runArgs);
				
				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					System.out.println("Mutant caught by: "+ datasetId);
					return true;
				}
				
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				return true;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	private Map<Integer,List<List<String>>> FindResultSetForMutants(Integer queryId, List<String> datasets, String query, String executablePath,String executableCommand, String fileName, String executableTableName ) {
		Map<Integer,List<List<String>>>  resultList = new HashMap<>();
		int i =0;
		for(String datasetId:datasets) {
			List<List<String>> resultListDB = new ArrayList<>();
			
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				String mutant = "select * from student_result";
				if(!executableTableName.equals(""))mutant = "select * from "+executableTableName;
				
				GenerateDataSet.loadSchema_NotTemp(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				String runArgs = executableCommand;
				String jarFile = executablePath + fileName;
				ExecuteJar.executeJar(jarFile,runArgs);
				
				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					List<String> resultInstructor = new ArrayList<String>();
					List<String> resultStudent = new ArrayList<String>();
					PreparedStatement ptsmtQuery=testConn.prepareStatement(query);
					ResultSet rsQuery=ptsmtQuery.executeQuery();
					while (rsQuery.next()) {
						
						//System.out.println("HELLO");
						ResultSetMetaData rsmd = rsQuery.getMetaData();
						 int columnsNumberQuery = rsmd.getColumnCount();
						 if(columnsNumberQuery != 0) {
					       for (int j = 1; j <= columnsNumberQuery; j++) {
					           String columnValue = rsQuery.getString(j);
					           resultInstructor.add(rsmd.getColumnName(j) + ": " +columnValue);
					       }
					       }
					       
				}
					
					PreparedStatement ptsmtMutant=testConn.prepareStatement(mutant);
					ResultSet rsMutant=ptsmtMutant.executeQuery();
					while (rsMutant.next()) {
						
						//System.out.println("HELLO");
						ResultSetMetaData rsmd = rsMutant.getMetaData();
						 int columnsNumberMutant = rsmd.getColumnCount();
						 if(columnsNumberMutant != 0) {
					       for (int j = 1; j <= columnsNumberMutant; j++) {
					           String columnValue = rsMutant.getString(j);
					           resultStudent.add(rsmd.getColumnName(j) + ": " +columnValue);
					       }
					       }
					       
				}
					resultListDB.add(resultInstructor);
					resultListDB.add(resultStudent);
					resultList.put(i,resultListDB);
					i++;
				}
				
				
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				return resultList;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return resultList;
	}
	private Map<Integer,List<List<String>>> FindResultSetForMutants(Integer queryId, List<String> datasets, String query, String mutant ) {
		Map<Integer,List<List<String>>>  resultList = new HashMap<>();
		int i =0;
		for(String datasetId:datasets) {
			List<List<String>> resultListDB = new ArrayList<>();
			
			try(Connection testConn=getTestConn(getIp(), getPort(), databaseName, databaseUser, databasePassword)){
				String filePath=queryId+"";
				
				
				GenerateDataSet.loadSchema_NotTemp(testConn, schema);
				//GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				
				
				
				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				System.out.println("dataset id: " + datasetId);
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
		
				
				if(rs.next()) {
					List<String> resultInstructor = new ArrayList<String>();
					List<String> resultStudent = new ArrayList<String>();
					PreparedStatement ptsmtQuery=testConn.prepareStatement(query);
					ResultSet rsQuery=ptsmtQuery.executeQuery();
					while (rsQuery.next()) {
						
						//System.out.println("HELLO");
						ResultSetMetaData rsmd = rsQuery.getMetaData();
						 int columnsNumberQuery = rsmd.getColumnCount();
						 if(columnsNumberQuery != 0) {
					       for (int j = 1; j <= columnsNumberQuery; j++) {
					           String columnValue = rsQuery.getString(j);
					           resultInstructor.add(rsmd.getColumnName(j) + ": " +columnValue);
					       }
					       }
					       
				}
					
					PreparedStatement ptsmtMutant=testConn.prepareStatement(mutant);
					ResultSet rsMutant=ptsmtMutant.executeQuery();
					while (rsMutant.next()) {
						
						//System.out.println("HELLO");
						ResultSetMetaData rsmd = rsMutant.getMetaData();
						 int columnsNumberMutant = rsmd.getColumnCount();
						 if(columnsNumberMutant != 0) {
					       for (int j = 1; j <= columnsNumberMutant; j++) {
					           String columnValue = rsMutant.getString(j);
					           resultStudent.add(rsmd.getColumnName(j) + ": " +columnValue);
					       }
					       }
					       
				}
					resultListDB.add(resultInstructor);
					resultListDB.add(resultStudent);
					resultList.put(i,resultListDB);
					i++;
				}
				
				
				/*
				if (rs.isBeforeFirst() ) {    
					//System.out.println("Hello from testmutantkilling");
				    return true;
				} 
				*/
			}catch(SQLException e) {
				e.printStackTrace();
				return resultList;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return resultList;
	}

	public  JSONObject returnXDataResult(String testQuery,String mutantQuery, String schema, String sampleData ) 
	{
		Map<Integer,String> queryMap = new HashMap<Integer,String>() ;
		Map<Integer,List<String>> mutantMap=new HashMap<Integer,List<String>>();
		List<String> mutants = new ArrayList<String>();
		List<String> datasetList = new ArrayList<String>();
		List<List<String>> mutantDBContent = new ArrayList<>();
		Map<Integer, List<List<String>>> resultList =new HashMap<>();
		String result ="";
		mutants.add(mutantQuery);
		queryMap.put(1, testQuery);
		mutantMap.put(1, mutants);
		
		List<String> errors=new ArrayList<String>();
		String exception ="";
		for(Integer queryId:queryMap.keySet()) {
			
			
			String query=queryMap.get(queryId);
			List<String> datasets;
			//Generate datasets
			datasets=generateDataSets(queryId,query);
			
			if(datasets==null || datasets.isEmpty()) {
				errors.add("Exception in generating datasets");
				exception = "Exception in generating datasets";
				continue;
			}
				
			//Check if DS0 works
			try {
				if(testBasicDataset(queryId,query)==false)
					errors.add("Basic datasets failed");
			} catch (Exception e)	{
				e.printStackTrace();
				errors.add("Exception in running query on basic test case");
				exception = "Exception in running query on basic test case";
				continue;
			}
			System.out.println("Error size after basic dataset: " + errors.size());
			
			//Check mutation killing
			for(String mutant:mutantMap.get(queryId))	{
				try {
					if(testMutantKilling(queryId, datasets, query, mutant)== true) {
						errors.add(mutant);
					datasetList= testEachMutant(queryId, datasets, query, mutant);
					mutantDBContent = allMutantDatabases(queryId,datasets,query,mutant);
					resultList = FindResultSetForMutants(queryId, datasets,query, mutant);
					}
					
				}catch (Exception e)	{
					e.printStackTrace();
					errors.add("Exception in killing mutant query:"+ mutant);
					exception ="Exception in killing mutant query";
				}
			}
			System.out.println("Error size after mutant killing: " + errors.size());
			if(errors.isEmpty()) {
				result="All Test Cases Passed";
			} else {
				result+="Following Test Cases Failed";
				for(Integer i=0;i<errors.size();i++) {
					result+=errors.get(i);
				}
			}
			
			
			
		}		
		
		JSONObject jsonObject = new JSONObject();
		if(!exception.equals("") || datasetList.contains("Errors while killing mutation")){
			jsonObject.put("mutantDetected","not applicable");
			JSONArray array = new JSONArray();			
			
			jsonObject.put("mutantDatasets", array);
			if(exception == "")exception = "Errors while killing mutation";
			jsonObject.put("exceptions", exception);
	}
	else {
		if(errors.isEmpty()) {
			jsonObject.put("mutantDetected","no");
			JSONArray array = new JSONArray();
			for (int i = 0; i < datasetList.size(); i++) {
				JSONObject mutantdbdetails = new JSONObject();
				mutantdbdetails.put("mutantDBType",datasetList.get(i));
				mutantdbdetails.put("mutantDBContent", mutantDBContent.get(i));
				
				array.add(mutantdbdetails);
			}
			
			jsonObject.put("mutantDatasets", array);
			jsonObject.put("exceptions", exception);
			
		
	}
		else {
			jsonObject.put("mutantDetected","yes");
			JSONArray array = new JSONArray();
			for (int i = 0; i < datasetList.size(); i++) {
				//JSONObject mutantdbdetails = new JSONObject();
				JSONObject jsonmutantdbdetails= new JSONObject();
				try {
				      java.lang.reflect.Field changeMap = jsonObject.getClass().getDeclaredField("map");
				      changeMap.setAccessible(true);
				      changeMap.set(jsonObject, new LinkedHashMap<>());
				      changeMap.setAccessible(false);
				    } catch (IllegalAccessException | NoSuchFieldException e) {
				     
				    }
				jsonmutantdbdetails.put("mutantDBType",datasetList.get(i));				
				jsonmutantdbdetails.put("mutantDBContent", mutantDBContent.get(i));	
				jsonmutantdbdetails.put("rsExtracted(Instructor)", resultList.get(i).get(0));
				jsonmutantdbdetails.put("rsHidden(Student)", resultList.get(i).get(1));				
				array.add(jsonmutantdbdetails);
			}
			//JSONObject json = (JSONObject) obj
			jsonObject.put("mutantDatasets", array);
			jsonObject.put("exceptions", exception);
			
		}
	
	//return result;
	}
		return jsonObject;
	}	
	
	
	
	public static void main(String[] args)	throws Exception{
		
		
		
		
		
		String basePath="src/main/java/test/universityTest";
		long startTime = System.currentTimeMillis();
		readFromJsonAPI(basePath);
		String databaseName = "";
		String databaseUser = "";
		String databasePassword = "";
		String databaseIP = "";
		String databasePort = "";		
		long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Total time taken by regression test is: ");
        System.out.print(elapsedTime);
		
	}

	//
	public static void readFromJsonAPI(String basePath) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONObject unmasqueOutput = (JSONObject) parser.parse(new FileReader(basePath+File.separator+"apiinput.json"));
		JSONObject extractedQueryJSON = (JSONObject)unmasqueOutput.get("extractedQuery(Instructor)");
		String testQuery = (String)extractedQueryJSON.get("sqlString");
		JSONObject inputQueryJSON = (JSONObject)unmasqueOutput.get("hiddenQuery(Student)");
		String isExecutable = (String)inputQueryJSON.get("isExecutable");
		String mutantQuery = (String)inputQueryJSON.get("sqlString");
		System.out.println(testQuery);
		databaseName = (String)unmasqueOutput.get("databaseName");
		databaseUser = (String)unmasqueOutput.get("databaseUser");
		databasePassword = (String)unmasqueOutput.get("databasePassword");
		setIp((String)unmasqueOutput.get("databaseIP"));
		setPort((String)unmasqueOutput.get("databasePort"));
		String sampleData ="";
		String schema ="";
		JSONArray tables = (JSONArray) unmasqueOutput.get("tables");
		for(int i=0;i < tables.size(); i++) {
			JSONObject table = (JSONObject)tables.get(i);
			String tableName = (String)table.get("name");
			String primaryKey = (String)table.get("primary_key");
			JSONArray columns = (JSONArray)table.get("columns");				
			schema += "create table "+ tableName + " (\n";
			for(int j = 0; j < columns.size(); j++) {
				JSONObject column = (JSONObject)columns.get(j);
				String colName = (String)column.get("name");
				String type = (String)column.get("type");
				String is_nullable = "NOT NULL";
				if((String)column.get("is_nullable") =="y")is_nullable ="";
				schema += colName + " " + type + " " + is_nullable +",\n";
			}
			schema += "PRIMARY KEY ("+primaryKey+")\n);\n\n";
		}
		RegressionTests r=new RegressionTests(schema,sampleData);
		JSONObject jsonobject =  new JSONObject();
		if(isExecutable.equals("n")) {
			jsonobject = r.returnXDataResult(testQuery, mutantQuery, schema, sampleData);
			}
		System.out.println(jsonobject);
		//System.out.println(r.returnXDataResult(testQuery, mutantQuery, schema, sampleData));
		
		
		
	}
	public static org.json.JSONObject readFromJsonAPI(JSONObject unmasqueOutput, String filename) throws IOException { 
		JSONParser parser = new JSONParser();
		//JSONObject unmasqueOutput = (JSONObject) parser.parse(new FileReader(basePath+File.separator+"apiinput.json"));
		JSONObject extractedQueryJSON = (JSONObject)unmasqueOutput.get("extractedQuery(Instructor)");
		String testQuery = (String)extractedQueryJSON.get("sqlString");
		JSONObject inputQueryJSON = (JSONObject)unmasqueOutput.get("hiddenQuery(Student)");
		String isExecutable = (String)inputQueryJSON.get("isExecutable");
		String executableType ="";
		String executablePath ="";
		String executableCommand="";
		String executableTableName = "";
		String mutantQuery = "";
		if(isExecutable.equals("n")) {
			mutantQuery = (String)inputQueryJSON.get("sqlString");
		}
		else
		{
			executableType = (String)inputQueryJSON.get("executableType");
			executablePath = (String)inputQueryJSON.get("executablePath");
			executableCommand = (String)inputQueryJSON.get("executableCommand");
			executableTableName = (String)inputQueryJSON.get("executableTableName");
		}
		System.out.println(testQuery);
		databaseName = (String)unmasqueOutput.get("databaseName");
		databaseUser = (String)unmasqueOutput.get("databaseUser");
		databasePassword = (String)unmasqueOutput.get("databasePassword");
		setIp((String)unmasqueOutput.get("databaseIP"));
		setPort((String)unmasqueOutput.get("databasePort"));
		String sampleData ="";
		String schema ="";
		JSONArray tables = (JSONArray) unmasqueOutput.get("tables");
		for(int i=0;i < tables.size(); i++) {
			JSONObject table = (JSONObject)tables.get(i);
			String tableName = (String)table.get("name");
			String primaryKey = (String)table.get("primary_key");
			JSONArray columns = (JSONArray)table.get("columns");				
			schema += "create table "+ tableName + " (\n";
			for(int j = 0; j < columns.size(); j++) {
				JSONObject column = (JSONObject)columns.get(j);
				String colName = (String)column.get("name");
				String type = (String)column.get("type");
				String is_nullable = "NOT NULL";
				if((String)column.get("is_nullable") =="y")is_nullable ="";
				schema += colName + " " + type + " " + is_nullable +",\n";
			}
			schema += "PRIMARY KEY ("+primaryKey+")\n);\n\n";
		}
		RegressionTests r=new RegressionTests(schema,sampleData);
		JSONObject jsonObject = new JSONObject();
		if(isExecutable.equals("n")) {
		jsonObject = r.returnXDataResult(testQuery, mutantQuery, schema, sampleData);
		}
		else {
		jsonObject = r.returnXDataResult(testQuery, schema, sampleData, executablePath, executableCommand, filename,executableTableName);
		}
		System.out.println(jsonObject);
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.add("mutantDBType");
		keyset.add("mutantDBContent");
		keyset.add("rsExtracted(Instructor)");
		keyset.add("rsHidden(Student)");
		org.json.JSONObject jsonObjectFormatted = MaintainOrder.formatJson(jsonObject, keyset);
		System.out.println(jsonObjectFormatted);
		return jsonObjectFormatted;
		
		
	}

	public static String getIp() {
		return ip;
	}

	public static void setIp(String ip) {
		RegressionTests.ip = ip;
	}

	public static String getPort() {
		return port;
	}

	public static void setPort(String port) {
		RegressionTests.port = port;
	}

	

}
