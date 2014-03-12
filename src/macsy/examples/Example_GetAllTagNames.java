package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

import java.util.List;

/**
 * It prints all available tags in the desired black board (specified in the settings file)
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * 
 * @author Ilias Flaounas
 */
public class Example_GetAllTagNames extends BaseModule {
	public Example_GetAllTagNames(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void runModuleCore() throws Exception {
		//Initialise 
		//Load Black Board of interest
		BlackBoard bb = _bbAPI.blackBoardLoad(MODULE_INPUT_BLACKBOARD );
		
		List<String> tagNames = bb.getAllTagNames();
		
		for(String tagName : tagNames)
			System.out.println( tagName );
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception 
	{
		Example_GetAllTagNames module = new Example_GetAllTagNames(args[0]);
		module.run();	
	}
}
