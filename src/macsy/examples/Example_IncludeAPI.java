package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

/* This line requires two JARs to be included: mongo.jar &
BlackBoardsAPI.jar */

/**
 * This a very simple program that initializes the API
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * @author Ilias Flaounas
 */
public class Example_IncludeAPI extends BaseModule{
	
	public Example_IncludeAPI(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		/* We create a single API object that we are to going to use throughout the
			lifetime of the program */
		/* The parameter is the filename (including path) of a connection settings
			file. */
		BlackBoard bb = _bbAPI.blackBoardLoad(MODULE_INPUT_BLACKBOARD );

	}
	/* API will throw Exceptions if something goes wrong. Be ready to catch
		them */
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */	
	public static void main(String[] args) throws Exception 
	{
		Example_IncludeAPI module = new Example_IncludeAPI(args[0]);
		module.run();
	}
}
