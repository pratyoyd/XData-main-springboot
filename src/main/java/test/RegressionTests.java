package test;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import testDataGen.GenerateDataSet;
import testDataGen.PopulateTestData;
import util.Configuration;
import util.TableMap;
import util.Utilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
				return datasetList;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return datasetList;
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
	
	public  JSONObject returnXDataResult(String testQuery,String mutantQuery, String schema, String sampleData ) 
	{
		Map<Integer,String> queryMap = new HashMap<Integer,String>() ;
		Map<Integer,List<String>> mutantMap=new HashMap<Integer,List<String>>();
		List<String> mutants = new ArrayList<String>();
		List<String> datasetList = new ArrayList<String>();
		String result ="";
		mutants.add(mutantQuery);
		queryMap.put(1, testQuery);
		mutantMap.put(1, mutants);
		
		List<String> errors=new ArrayList<String>();
		for(Integer queryId:queryMap.keySet()) {
			
			
			String query=queryMap.get(queryId);
			List<String> datasets;
			//Generate datasets
			datasets=generateDataSets(queryId,query);
			
			if(datasets==null || datasets.isEmpty()) {
				errors.add("Exception in generating datasets");
				continue;
			}
				
			//Check if DS0 works
			try {
				if(testBasicDataset(queryId,query)==false)
					errors.add("Basic datasets failed");
			} catch (Exception e)	{
				e.printStackTrace();
				errors.add("Exception in running query on basic test case");
				continue;
			}
			System.out.println("Error size after basic dataset: " + errors.size());
			
			//Check mutation killing
			for(String mutant:mutantMap.get(queryId))	{
				try {
					if(testMutantKilling(queryId, datasets, query, mutant)== true)
						errors.add(mutant);
					datasetList= testEachMutant(queryId, datasets, query, mutant);
				}catch (Exception e)	{
					e.printStackTrace();
					errors.add("Exception in killing mutant query:"+mutant);
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
		System.out.println(result);
		JSONObject jsonObject = new JSONObject();
		if(errors.isEmpty())jsonObject.put("mutantDetected","no");
		else {
			jsonObject.put("mutantDetected","yes");
			JSONArray array = new JSONArray();
			for(String dataset : datasetList)
			array.add(dataset);
			jsonObject.put("mutantDatasets", array);
			
		}
		return jsonObject;
		//return result;
	}
	 
	
	
	public static void main(String[] args)	throws Exception{
		
		
		
		
		
		String basePath="src/main/java/test/universityTest";
		readFromJsonAPI(basePath);
		String databaseName = "";
		String databaseUser = "";
		String databasePassword = "";
		String databaseIP = "";
		String databasePort = "";
		//Path of file containing schema
		//String schemaFile="test/universityTest/DDL_Q3_1.sql";
		//Path of file containing sampleData
		//String sampleDataFile="test/universityTest/sampleData_Q3.sql";
		//String sampleDataFile="test/universityTest/sampleData.sql";
		/* runtime analysis for regression test */
		long startTime = System.currentTimeMillis();
		/*
		String testQuery = "SELECT L_ORDERKEY, SUM(L_EXTENDEDPRICE* (1 - L_DISCOUNT)) AS REVENUE, O_ORDERDATE, O_SHIPPRIORITY FROM customer, lineitem, orders WHERE C_CUSTKEY = O_CUSTKEY AND L_ORDERKEY = O_ORDERKEY AND C_MKTSEGMENT ='BUILDING' AND O_ORDERDATE <= '1995-03-14' AND L_SHIPDATE >= '1995-03-16' GROUP BY L_ORDERKEY, O_SHIPPRIORITY, O_ORDERDATE ORDER BY REVENUE DESC, O_ORDERDATE ASC LIMIT 5";
		String mutantQuery = "SELECT L_ORDERKEY, SUM(L_EXTENDEDPRICE* (1 - L_DISCOUNT)) AS REVENUE, O_ORDERDATE, O_SHIPPRIORITY FROM customer, lineitem, orders WHERE C_CUSTKEY = O_CUSTKEY AND L_ORDERKEY = O_ORDERKEY AND C_MKTSEGMENT ='BUILDING' AND O_ORDERDATE <= '1995-03-14' AND L_SHIPDATE >'1995-03-16' GROUP BY L_ORDERKEY, O_SHIPPRIORITY, O_ORDERDATE ORDER BY REVENUE DESC, O_ORDERDATE ASC LIMIT 5";
		
		//String mutantQuery = "SELECT L_ORDERKEY, SUM(L_EXTENDEDPRICE * (1 - L_DISCOUNT)) AS REVENUE, O_ORDERDATE, O_SHIPPRIORITY FROM customer, orders, lineitem WHERE C_CUSTKEY = O_CUSTKEY AND L_ORDERKEY = O_ORDERKEY AND C_MKTSEGMENT ='BUILDING' AND O_ORDERDATE < '1995-03-15' AND L_SHIPDATE > '1995-03-15' GROUP BY L_ORDERKEY,  O_ORDERDATE , O_SHIPPRIORITY ORDER BY REVENUE DESC, O_ORDERDATE LIMIT 10";
		String  schema =
		"CREATE   TABLE region  ( R_REGIONKEY  INTEGER NOT NULL,\n"
		+ "                            R_NAME       CHAR(25) NOT NULL,\n"
		+ "                            R_COMMENT    VARCHAR(152),\n"
		+ "                            primary key (R_REGIONKEY)                           \n"
		+ "                            );\n"
		+ "                            \n"
		+ "CREATE   TABLE nation  ( N_NATIONKEY  INTEGER NOT NULL,\n"
		+ "                            N_NAME       CHAR(25) NOT NULL,\n"
		+ "                            N_REGIONKEY  INTEGER NOT NULL,\n"
		+ "                            N_COMMENT    VARCHAR(152),\n"
		+ "                            primary key (N_NATIONKEY)                             \n"
		+ "                            );\n"
		+ "\n"
		+ "\n"
		+ "CREATE   TABLE part  ( P_PARTKEY     INTEGER NOT NULL,\n"
		+ "                          P_NAME        VARCHAR(55) NOT NULL,\n"
		+ "                          P_MFGR        CHAR(25) NOT NULL,\n"
		+ "                          P_BRAND       CHAR(10) NOT NULL,\n"
		+ "                          P_TYPE        VARCHAR(25) NOT NULL,\n"
		+ "                          P_SIZE        INTEGER NOT NULL,\n"
		+ "                          P_CONTAINER   CHAR(10) NOT NULL,\n"
		+ "                          P_RETAILPRICE numeric(15,2) NOT NULL,\n"
		+ "                          P_COMMENT     VARCHAR(23) NOT NULL ,\n"
		+ "                          PRIMARY KEY (P_PARTKEY)\n"
		+ "                          );\n"
		+ "\n"
		+ "CREATE   TABLE supplier ( S_SUPPKEY     INTEGER NOT NULL,\n"
		+ "                             S_NAME        CHAR(25) NOT NULL,\n"
		+ "                             S_ADDRESS     VARCHAR(40) NOT NULL,\n"
		+ "                             S_NATIONKEY   INTEGER NOT NULL,\n"
		+ "                             S_PHONE       CHAR(15) NOT NULL,\n"
		+ "                             S_ACCTBAL     numeric(15,2) NOT NULL,\n"
		+ "                             S_COMMENT     VARCHAR(101) NOT NULL,\n"
		+ "                             PRIMARY KEY (S_SUPPKEY) );\n"
		+ "\n"
		+ "CREATE   TABLE partsupp ( PS_PARTKEY     INTEGER NOT NULL,\n"
		+ "                             PS_SUPPKEY     INTEGER NOT NULL,\n"
		+ "                             PS_AVAILQTY    INTEGER NOT NULL,\n"
		+ "                             PS_SUPPLYCOST  numeric(15,2)  NOT NULL,\n"
		+ "                             PS_COMMENT     VARCHAR(199) NOT NULL,\n"
		+ "                             PRIMARY KEY (PS_PARTKEY, PS_SUPPKEY)\n"
		+ "                             );\n"
		+ "\n"
		+ "CREATE   TABLE customer ( C_CUSTKEY     INTEGER NOT NULL,\n"
		+ "                             C_NAME        VARCHAR(25) NOT NULL,\n"
		+ "                             C_ADDRESS     VARCHAR(40) NOT NULL,\n"
		+ "                             C_NATIONKEY   INTEGER NOT NULL,\n"
		+ "                             C_PHONE       CHAR(15) NOT NULL,\n"
		+ "                             C_ACCTBAL     numeric(15,2)   NOT NULL,\n"
		+ "                             C_MKTSEGMENT  CHAR(10) NOT NULL,\n"
		+ "                             C_COMMENT     VARCHAR(117) NOT NULL,\n"
		+ "                             PRIMARY KEY (C_CUSTKEY) );\n"
		+ "\n"
		+ "CREATE   TABLE orders ( O_ORDERKEY       INTEGER NOT NULL,\n"
		+ "                           O_CUSTKEY        INTEGER NOT NULL,\n"
		+ "                           O_ORDERSTATUS    CHAR(1) NOT NULL,\n"
		+ "                           O_TOTALPRICE     numeric(15,2) NOT NULL,\n"
		+ "                           O_ORDERDATE      DATE NOT NULL,\n"
		+ "                           O_ORDERPRIORITY  CHAR(15) NOT NULL,  \n"
		+ "                           O_CLERK          CHAR(15) NOT NULL, \n"
		+ "                           O_SHIPPRIORITY   INTEGER NOT NULL,\n"
		+ "                           O_COMMENT        VARCHAR(79) NOT NULL,\n"
		+ "                           PRIMARY KEY (O_ORDERKEY) );\n"
		+ "\n"
		+ "CREATE   TABLE lineitem ( L_ORDERKEY    INTEGER NOT NULL,\n"
		+ "                             L_PARTKEY     INTEGER NOT NULL,\n"
		+ "                             L_SUPPKEY     INTEGER NOT NULL,\n"
		+ "                             L_LINENUMBER  INTEGER NOT NULL,\n"
		+ "                             L_QUANTITY    numeric(15,2) NOT NULL,\n"
		+ "                             L_EXTENDEDPRICE  numeric(15,2) NOT NULL,\n"
		+ "                             L_DISCOUNT    numeric(15,2) NOT NULL,\n"
		+ "                             L_TAX         numeric(15,2) NOT NULL,\n"
		+ "                             L_RETURNFLAG  CHAR(1) NOT NULL,\n"
		+ "                             L_LINESTATUS  CHAR(1) NOT NULL,\n"
		+ "                             L_SHIPDATE    DATE NOT NULL,\n"
		+ "                             L_COMMITDATE  DATE NOT NULL,\n"
		+ "                             L_RECEIPTDATE DATE NOT NULL,\n"
		+ "                             L_SHIPINSTRUCT CHAR(25) NOT NULL,\n"
		+ "                             L_SHIPMODE     CHAR(10) NOT NULL,\n"
		+ "                             L_COMMENT      VARCHAR(44) NOT NULL,\n"
		+ "                             PRIMARY KEY (L_ORDERKEY, L_LINENUMBER));";
		String sampleData = 
				"delete from part;\n"
				+ "delete from supplier;\n"
				+ "delete from partsupp;\n"
				+ "delete from customer;\n"
				+ "delete from nation;\n"
				+ "delete from lineitem;\n"
				+ "delete from region;\n"
				+ "delete from orders;\n"
				+ "insert into region values ('0','AFRICA','lar deposits. blithely final packages cajole. regular waters are final requests. regular accounts are according to' );\n"
				+ "insert into region values ('1','AMERICA','hs use ironic, even requests. s');\n"
				+ "insert into nation values ('15','MOROCCO','0','rns. blithely bold courts among the closely regular packages use furiously bold platelets?');\n"
				+ "insert into nation values ('17','PERU','1','platelets. blithely pending dependencies use fluffily across the even pinto beans. carefully silent accoun');\n"
				+ "insert into nation values ('1','ARGENTINA','1','al foxes promise slyly according to the regular accounts. bold requests alon');\n"
				+ "insert into nation values ('2','BRAZIL','1','y alongside of the pending deposits. carefully special packages are about the ironic forges. slyly special');\n"
				+ "insert into nation values ('3','CANADA','1','eas hang ironic, silent packages. slyly regular packages are furiously over the tithes. fluffily bold');\n"
				+ "insert into customer values ('32','Customer000000032','jD2xZzi UmId,DCtNBLXKj9q0Tlp2iQ6ZcO3J','15','25-430-914-2194','3471.53','BUILDING','cial ideas. final, furious requests across the e');\n"
				+ "insert into customer values('33','Customer000000033','qFSlMuLucBmx9xnn5ib2csWUweg D','17','27-375-391-1280','78.56','AUTOMOBILE','s. slyly regular accounts are furiously. carefully pending requests');\n"
				+ "insert into customer values('34','Customer000000034','Q6G9wZ6dnczmtOx509xgE,M2KV','1','25-344-968-5422','8589.70','HOUSEHOLD','nder against the even, pending accounts. even');\n"
				+ "insert into customer values('35','Customer000000035','Q6G9wZ6dnczmtOx509xgE,M2KV','2','25-344-968-5422','858.70','BUILDING','nder against the even, pending accounts. even');\n"
				+ "insert into customer values('36','Customer000000036','Q6G9wZ6dnczmtOx509xgE,M2KV','3','25-344-968-5422','85.70','BUILDING','nder against the even, pending accounts. even');\n"
				+ " insert into part values('1','goldenrod lavender spring chocolate lace','Manufacturer1','Brand13','PROMO BURNISHED COPPER','7','JUMBO PKG','901.00','ly. slyly ironi');\n"
				+ "insert into part values('2','blush thistle blue yellow saddle','Manufacturer1','Brand13','LARGE BRUSHED BRASS','1','LG CASE','902.00','lar accounts amo');\n"
				+ "insert into part values('4','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('3','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('5','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('6','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('7','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('8','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('9','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into part values('10','spring green yellow purple cornsilk','Manufacturer4','Brand42','STANDARD POLISHED BRASS','21','WRAP CASE','903.00','egular deposits hag');\n"
				+ "insert into supplier values('1','Supplier000000001',' N kD4on9OM Ipw3,gf0JBoQDd7tgrzrddZ','17','27-918-335-1736','5755.94','each slyly above the careful');\n"
				+ "insert into supplier values('2','Supplier000000002','89eJ5ksX3ImxJQBvxObC,','15','15-679-861-2259','4032.68',' slyly bold instructions. idle dependen');\n"
				+ "insert into supplier values('4','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','15','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('3','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','1','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('5','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','15','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('6','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','1','11-383-516-1199','3987.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('7','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','1','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('8','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','17','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('9','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','2','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into supplier values('10','Supplier000000003','q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3','3','11-383-516-1199','4192.40','blithely silent requests after the express dependencies are slu');\n"
				+ "insert into partsupp values('4','4','3325','771.64',', even theodolites. regular, final theodolites eat after the carefully pending foxes. furiously regular deposits sleep slyly. carefully bold realms above the ironic dependencies haggle careful');\n"
				+ "insert into partsupp values('1','1','8076','993.49','ven ideas. quickly even packages print. pending multipliers must have to are fluff');\n"
				+ "insert into partsupp values('2','2','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('3','3','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('5','5','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('6','6','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('7','7','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even exc)uses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('8','8','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('9','9','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('10','10','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into partsupp values('10','3','3956','337.09','after the fluffily ironic deposits? blithely special dependencies integrate furiously even excuses. blithely silent theodolites could have to haggle pending, express requests fu');\n"
				+ "insert into orders values ('1','32','O','173665.47','1995-01-02','5-LOW','Clerk000000951','0','nstructions sleep furiously among ');\n"
				+ "insert into orders values ('2','33','O','46929.18','1996-03-01','1-URGENT','Clerk000000880','0',' foxes. pending accounts at the pending, silent asymptot');\n"
				+ "insert into orders values ('5','34','F','144659.20','1993-01-30','5-LOW','Clerk000000925','0','quickly. bold deposits sleep slyly. packages use slyly');\n"
				+ "insert into orders values ('3','32','F','193846.25','1993-10-14','5-LOW','Clerk000000955','0','sly final accounts boost. carefully regular ideas cajole carefully. depos');\n"
				+ "insert into orders values ('4','33','O','32151.78','1996-01-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into orders values ('6','36','O','32151.78','1995-01-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into orders values ('7','35','O','32151.78','1995-02-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into orders values ('8','36','O','32151.78','1996-03-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into orders values ('9','35','O','32151.78','1995-01-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into orders values ('10','36','O','32151.78','1997-02-11','5-LOW','Clerk000000124','0','sits. slyly regular warthogs cajole. regular, regular theodolites acro');\n"
				+ "insert into lineitem values ('1','1','1','1','17','21168.23','0.04','0.02','N','O','1995-03-20','1995-02-12','1995-03-22','DELIVER IN PERSON','TRUCK','egular courts above the');\n"
				+ "insert into lineitem values ('2','2','2','2','36','45983.16','0.09','0.06','N','O','1996-04-12','1996-02-28','1996-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('5','5','5','5','36','45983.16','0.09','0.06','N','O','1993-04-12','1993-02-28','1993-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('3','3','3','3','28','28955.64','0.09','0.06','N','O','1993-12-21','1993-10-30','1993-12-26','NONE','AIR','lites. fluffily even de');\n"
				+ "insert into lineitem values ('4','4','4','4','36','45983.16','0.09','0.06','N','O','1996-04-15','1996-02-28','1996-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('6','6','6','6','36','45983.16','0.09','0.06','N','O','1995-04-12','1995-02-28','1995-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('7','7','7','7','36','45983.16','0.09','0.06','N','O','1996-04-12','1996-02-28','1996-04-19','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('8','8','8','8','36','45983.16','0.09','0.06','N','O','1996-04-12','1996-02-28','1996-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('9','9','9','9','36','45983.16','0.09','0.06','N','O','1995-04-12','1995-02-28','1995-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');\n"
				+ "insert into lineitem values ('10','10','10','10','36','45983.16','0.09','0.06','N','O','1997-04-12','1997-02-28','1997-04-20','TAKE BACK RETURN','MAIL','ly final dependencies: slyly bold');";
		RegressionTests r=new RegressionTests(schema,sampleData);
		System.out.println(r.returnXDataResult(testQuery, mutantQuery, schema, sampleData));
	 	*/		
		//System.out.println("Starting time of regression test is:");
        //System.out.println(startTime);
		/*
		RegressionTests r=new RegressionTests(basePath,schemaFile,sampleDataFile);
		Map<Integer,List<String>> errorsMap=r.runRegressionTests();
		
		String errors=""; 
		if(errorsMap==null)
			System.out.println("Exception......");
		else if(errorsMap.isEmpty()) {
			errors="All Test Cases Passed";
		} else {
			errors+="Following Test Cases Failed";
			for(Integer key:errorsMap.keySet()) {
				errors=key+"|";
				for(String err:errorsMap.get(key)) {
					errors+=err+"|";
				}
				errors+="\n";
			}
		}
		Utilities.writeFile(basePath+File.separator+"test_result.log", errors);
		System.out.println("Errors" +errors);
		
		//System.out.println("Stopping time of regression test is: ");
	    //System.out.println(stopTime);*/
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
		JSONObject jsonobject = r.returnXDataResult(testQuery, mutantQuery, schema, sampleData);
		System.out.println(jsonobject);
		//System.out.println(r.returnXDataResult(testQuery, mutantQuery, schema, sampleData));
		
		
	}
	public static JSONObject readFromJsonAPI(JSONObject unmasqueOutput) throws IOException { 
		JSONParser parser = new JSONParser();
		//JSONObject unmasqueOutput = (JSONObject) parser.parse(new FileReader(basePath+File.separator+"apiinput.json"));
		JSONObject extractedQueryJSON = (JSONObject)unmasqueOutput.get("extractedQuery(Instructor)");
		String testQuery = (String)extractedQueryJSON.get("sqlString");
		JSONObject inputQueryJSON = (JSONObject)unmasqueOutput.get("hiddenQuery(Student)");
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
		JSONObject jsonobject = r.returnXDataResult(testQuery, mutantQuery, schema, sampleData);
		System.out.println(jsonobject);
		//System.out.println(r.returnXDataResult(testQuery, mutantQuery, schema, sampleData));
		return jsonobject;
		
		
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
