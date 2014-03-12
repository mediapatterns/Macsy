package macsy.examples;

import macsy.module.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;

/**
 * In this example we will compute the average length of a specific field of documents 
 * in a period of interest
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELDS=The name of the field the average of those values we want to compute
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * @author Ilias Flaounas
 */
public class Example_MeasureAverageContentLength extends BaseModule {
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public Example_MeasureAverageContentLength(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );

		// get the user choice for dates and transform them into the desirable form
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));

		System.out.println("Searching articles...");
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);

		// The field of interest is the output of the previous module
		String fieldName = MODULE_INPUT_FIELDS;            

		double sum = 0;
		int numOfArticles = 0;
		BBDoc s;
		while((s = DocSet.getNext())!=null)
		{
			// For each article we read the field of interest and we add it to the variable
			Integer articleLength =  s.getFieldInt( fieldName );
			if(articleLength!=null) {
				sum += articleLength.doubleValue();
				numOfArticles++;
			}
		}
		// In the end we compute the average article length
		System.out.println("Average length = "+ sum / numOfArticles);
	}

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */	
	public static void main(String[] args) throws Exception 
	{
		Example_MeasureAverageContentLength module = new Example_MeasureAverageContentLength(args[0]);
		module.run();
	}
}
