package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * In this example we will find a feed and find articles that were published in it
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_TAGS=Run module only on docs that have this tag.
 * INPUT_FIELDS=The name of the field whose values we want to print
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * INPUT_CLASS_TAGS=The name of the class (if the tag exists the class is set to 1 else to 0)
 * @author Ilias Flaounas
 */
public class Example_GetFeeds extends BaseModule {	
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public Example_GetFeeds(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		//Prepare TAG 
		String BlackBoards[] = MODULE_INPUT_BLACKBOARD.split(",");
		//List<String> BlackBoardsNames = new LinkedList<String>();		
		for(String BlackBoardsName : BlackBoards) 
		{
			//Load Black Board of interest (with data based)
			BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(BlackBoardsName );
			//finds the first document
			BBDocSet DocSet = bb.findDocsByTagsSet(null, null, 1);
			BBDoc s = DocSet.getNext();
			//BBDoc feed = bb.findDocByID(10);
			
			//System.out.println( "URL="+feed.getXmlLink() );
			System.out.println( "Title="+s.getFieldString("T"));
			System.out.println( "Description="+s.getFieldString("D"));
			System.out.println( "Content="+s.getFieldString("C"));
			//System.out.println( "Outlet ID=" + feed.getOutletID() );

			List features = (List) s.getField( MODULE_INPUT_FIELDS );
			if(features != null)
				for(int f=0;f<features.size() ; f+=2) 
					System.out.printf("%d:%.3f ", features.get(f), features.get(f+1));
			System.out.println();
			
			// Period of interest
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
			Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
			Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
			
			// We query for all articles in a specific time period from a specific outlet
			DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);
			
			// as long as there are still docs in the BBDocSet
			// get the next doc int the BBDocSet
			while((s = DocSet.getNext())!=null)
				System.out.println(s.getField("T"));
		}	
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */	
	public static void main(String[] args) throws Exception 
	{
		Example_GetFeeds module = new Example_GetFeeds(args[0]);
		module.run();
	}
}
