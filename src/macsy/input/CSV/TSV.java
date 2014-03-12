package macsy.input.CSV;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.module.BaseModule;

/**
 * Imports a Comma Separated Values (CSV) file into a blackboard.
 * Columns are features and lines are data points.
 * If cells are numbers they are converted to doubles. 
 * 
 * Input:
 * CSV_FILENAME=The path to a CSV file.
 * 
 * Output:
 * OUTPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * OUTPUT_TAGS=The name of a single tag that will be applied to all new inserted data points.
 * ==
 * OUTPUT_FIELDS are specified inside the CSV as first line.
 * 
 * @author flaounas
 *
 */
public class TSV extends BaseModule {

	static final String PROPERTY_CSV_FILENAME = "CSV_FILENAME";

	public TSV(String propertiesFilename ) throws Exception 
	{
		super(propertiesFilename);

	}

	public void runModuleCore() throws Exception 
	{
		String csvFilename = this.getProperty(PROPERTY_CSV_FILENAME);
		File csvFile = new File(csvFilename);
		if(!csvFile.exists())
			return;

		//Load Black Board of interest
		BlackBoard bb = _bbAPI.blackBoardLoad(  MODULE_OUTPUT_BLACKBOARD );

		//Prepare TAG 
		String tagNames[] = MODULE_OUTPUT_TAGS.split(",");
		List<Integer> tagIDsToAdd = new LinkedList<Integer>();
		for(String tagName : tagNames) 
		{
			int tagID = bb.getTagID(tagName);
			if( tagID==0)
				tagID = bb.insertNewTag(tagName);
			tagIDsToAdd.add(tagID);

		}


		BufferedReader input = new BufferedReader( new FileReader(csvFile) );

		//LOAD HEADER = COLUMN LABELS
		String line = input.readLine();  //First line = HEADER OF CSV = COLUMN LABELS 
		String column_labels[] = line.split(",");

		Double v = null; 
		
		int linesRead = 0;
		int linesWritten  =0;
		//Start reading data (one point per line)
		while (( line = input.readLine()) != null)
		{
			linesRead++;
			
			if((MODULE_DATA_PROCESS_LIMIT>0) && (linesRead > this.MODULE_DATA_PROCESS_LIMIT))
				break;
			
			String values[] = line.split(",");
			BBDoc doc = new BBDoc();

			for(int i=0; i< values.length ; i++)
			{
				v = null; 
				try {
					v= Double.parseDouble(values[i]); 
				} catch(Exception e) {}	//Not number
				if(v==null)
					doc.setField(column_labels[i], values[i] ); //Add as String
				else if(column_labels[i].length() > 0)
					doc.setField(column_labels[i], v ); //Add as Double
			}
			doc.setTags(tagIDsToAdd);
			
			bb.insertNewDoc(doc);
			linesWritten++;
		}
		input.close();
		
		this.saveModuleResults(linesRead, linesWritten);
	}

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		TSV module = new TSV(args[0]);
		module.run();
	}


}
