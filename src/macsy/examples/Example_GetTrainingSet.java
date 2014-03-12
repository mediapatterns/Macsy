package macsy.examples;

import macsy.module.*;
import macsy.blackBoardsSystem.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * In this example we will create a simple training set for a classification task
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
public class Example_GetTrainingSet extends BaseModule {
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	static final String PROPERTY_CLASS_TAG = "INPUT_CLASS_TAGS";
	
	public Example_GetTrainingSet(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void runModuleCore() throws Exception {
				
		// get the user choice for dates and transform them into the desirable form
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
				
		/* We first query DB for the english articles of January 1st, 2010 (up to 100
		in this example) */
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		//Prepare TAG 
		String tagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> Tag_List = new LinkedList<Integer>();		
		for(String tagName : tagNames) 
		{
			if(!tagName.equals("")){
				int tagID = bb.getTagID(tagName);
				if( tagID==0){
					System.err.println("No known input tag");
					System.exit(0);
				}
				Tag_List.add(tagID);
			}
		}

		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);
	
		// as long as there are still docs in the BBDocSet
		// get the next doc int the BBDocSet
		BBDoc s;
		while((s = DocSet.getNext())!=null)
		{
			if(s.getTagIDs()!=null && s.getTagIDs().containsAll(Tag_List)){
				String featuresFieldName = MODULE_INPUT_FIELDS;            
				String labelTagName = this.getProperty(PROPERTY_CLASS_TAG);
				Integer labelTagID = bb.getTagID(labelTagName);
				
				List<Double> features = (List<Double>) s.getField( featuresFieldName);
				if(features != null)
				{
					if(s.getTagIDs().contains(labelTagID))
						System.out.print(1 +" ");
					else
						System.out.print(0 +" ");
									
					for(int f=0;f<features.size() ; f+=2)
						System.out.printf("%d:%.3f ",(int)features.get(f).doubleValue(), features.get(f+1).doubleValue());
					/* The first number is the label and then a features vector in sparse format
					(featureID:value pairs for non-zero features). */

					System.out.println();
				}
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
		Example_GetTrainingSet module = new Example_GetTrainingSet(args[0]);
		module.run();
	}
}
