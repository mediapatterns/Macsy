package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * In this example the goal is to find the documents with a specified tag. 
 * Use your initials for the tagName
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_TAGS=The tag we want to query.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * @author Ilias Flaounas
 */
public class Example_TagsQuerying extends BaseModule {
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public Example_TagsQuerying(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		//Initialize
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
				
		//CREATE NEW TAG
		String tagName = MODULE_INPUT_TAGS;
		Integer tagID = bb.getTagID(tagName);
		
		
		/* We get the ID of the tag and check if it�s already in DB. If it is not in the
		DB then we add it */
		if(tagID.equals(BlackBoard.TAG_NOT_FOUND) )
			tagID = bb.insertNewTag(tagName);
		System.out.println("Tag "+ tagName + " has ID=" +  tagID);		
				
		// Subset of articles we are interested in
		/* We make a list of tags of interest. In this example we use only the tag we
			created before */ 
		List<Integer> tags = new LinkedList<Integer>();
		tags.add( tagID );

		// Period of interest
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
				
				/* We find articles within specific time period, from specific outlet, that have
					the specific tag */
				//BBDocArticleSet articleSet = bbAPI.articles.findArticleSet(fromDate,toDate, outletIDs, tags , null, 0, 0);
		// We query for all articles in a specific time period from a specific outlet
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);
		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;
		BBDoc s;
		//Print article IDs
		System.out.println("Articles with tag "+ tagName);
		while((s = DocSet.getNext())!=null)
		{
			dataRead++;
			if(s.getAllTagIDs()!=null && s.getAllTagIDs().containsAll(tags)){
				dataProcessed++;
				System.out.println( s.getID() );
				//System.out.println(article.getFieldString("LD"));
			}
			
		}
		
		// display the number of input items and the number of output items
		this.saveModuleResults(dataRead, dataProcessed);
	}

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception 
	{
		Example_TagsQuerying module = new Example_TagsQuerying(args[0]);
		module.run();		
	}
}
