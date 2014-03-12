package macsy.examples;

import macsy.module.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;

/**
 * In this example we print the content of an document
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * @author Ilias Flaounas
 */
public class Example_ReadArticle extends BaseModule {
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public Example_ReadArticle(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );

		// get the user choice for dates and transform them into the desirable form
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy:hh-mm-ss");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));

		
		/* We query for one article in the period of interest. The result is an object
			of type BBDocArticleSet */
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);
		
		/* We get an article out of the set using function getNext(). The function
		returns an object of type BBDocArticle */
		BBDoc s = DocSet.getNext();
		/* We check if result is null. A null value means that the BBDocArticleSet
			contains no more results */
		if(s !=null)
		{
			System.out.println( "ID="+s.getID() );
			System.out.println( "TITLE="+s.getField("T"));
			System.out.println( "DESCRIPTION="+s.getField("D"));
			System.out.println( "CONTENT="+s.getField("C"));		

			System.out.print( "Tags = " );
			// We get a list of article Tag IDs
			List<Integer> tagIDs = s.getAllTagIDs();
			// We print out the name of each tag in the list
			for(Integer tagID : tagIDs)	{
				String tagName = bb.getTagName(tagID);
				System.out.print(tagName+" ");
			}
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
		Example_ReadArticle module = new Example_ReadArticle(args[0]);
		module.run();
	}
}
