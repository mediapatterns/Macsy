package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

import java.util.Date;


/**
 * It prints the date of the first and the last document and also the number of 
 * the documents in the database.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * 
 * @author Ilias Flaounas
 */
public class Example_ArticlesInDB extends BaseModule {
	public Example_ArticlesInDB(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		// We find the minimum date of articles in DB
		Date fromDate  = bb.getMinDocDate();
		// We find the maximum date of articles in DB
		Date toDate  = bb.getMaxDocDate();
		// We count the total number of articles in DB
		int count = bb.getNumberOfDocs();
		
		System.out.println("First article date = " + fromDate);
		System.out.println("Last article date  = " + toDate);
		System.out.println("Total number of articles = " + count);
	
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */	
	public static void main(String[] args) throws Exception 
	{
		Example_ArticlesInDB module = new Example_ArticlesInDB(args[0]);
		module.run();
	}
}
