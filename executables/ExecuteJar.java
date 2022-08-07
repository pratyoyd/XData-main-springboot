/**
 * 
 */
package helper;

import java.io.IOException;

/**
 * @author manish
 *
 */
public class ExecuteJar 
{
	
	public static void executeJar(String jarFilePath, String args) 
	{
		int exitVal;
	    
	    String str_cmd = "java -jar " + jarFilePath + " " + args;
	    try {
	        final Runtime re = Runtime.getRuntime();
	        final Process command = re.exec(str_cmd);
	        // Wait for the application to Finish
	        command.waitFor();
	        exitVal = command.exitValue();
	        if (exitVal != 0) {
	            throw new IOException("Failed to execure jar " );
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String runArgs = "-o \"5432\" -d \"xdatadb\" -u \"xdatauser\" -p Xdatauser@123\"\" -l \"localhost\" ";
		 
		 String jarFile = "/Users/manish/executable/Q3.jar";
		    ExecuteJar.executeJar(jarFile, runArgs);;
		    
	}

}
