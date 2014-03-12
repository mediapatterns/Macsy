package macsy.module.binaryRepresentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.module.BaseModule;

public class BinaryRepresentation extends BaseModule{
	// temporal variables to hold information given by the user
	static final String PROPERTY_INPUT_VOCABULARY_FILENAME = "INPUT_VOCABULARY_FILENAME";
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	static final String PROPERTY_THRESHOLD = "THRESHOLD";
	
	
	public BinaryRepresentation(String propertiesFilename ) throws Exception 
	{
		super(propertiesFilename);
	}
	
	/**
	 * This function takes a file name as parameter and it returns its contents 
	 * @param filename
	 * @return the string that was read by the file
	 */
	private Map<String, Double> LoadFileToMap(String filename) 
	{
		StringBuffer stringBuffer = new StringBuffer();
		Map<String, Double> wordMap = new HashMap<String, Double>();
		
		try {
			File file = new File(filename);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				String[] splittedLine= line.split(" ");
				if(splittedLine.length==1)
					wordMap.put(splittedLine[0], 1.0);
				else
					wordMap.put(splittedLine[0], Double.valueOf(splittedLine[1]));
				stringBuffer.append("\n");
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wordMap;
	}


	public void runModuleCore() throws Exception 
	{	
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		//Input Fields
		List<String> inputFields = new ArrayList<String>();
		String inFields_toks[] = MODULE_INPUT_FIELDS.split(",");
		for(String f : inFields_toks)
			inputFields.add(f);
		
		//Prepare output tags 
		int outputTagID = 0;
		String outTagNames[] = MODULE_OUTPUT_TAGS.split(",");
		List<Integer> outTag_List = new LinkedList<Integer>(); //Variable name clarification
		for(String tagName : outTagNames) 
		{
			if(!tagName.equals("")){
				outputTagID = bb.getTagID(tagName);
				// if the tag not already exists insert new tag 
				if( outputTagID==0)
					outputTagID=bb.insertNewTag(MODULE_OUTPUT_TAGS);
				outTag_List.add(outputTagID);
			}
		}
		
		//Initialize indexer (Input Voc)
		String vocabularyFilename = this.getProperty(PROPERTY_INPUT_VOCABULARY_FILENAME);
		
		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;
		
		// create map which maps each word with its weight
		Map<String, Double> wordsAndWeights = LoadFileToMap(vocabularyFilename);
		
		// make the day in the desired format
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		// get the desired period of time
		Date fromDate = dateFormat.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = dateFormat.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
		
		// get the article of interest
		// find the documents in a desired period of time with the Tag list of interest 
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);

		BBDoc s;
		while((s = DocSet.getNext())!=null){
			// initialize the sum needed for the inner product
			double sum = 0.0;
			SortedSet<String> word_counts=new TreeSet<String>();
			
			dataRead++;
			// initialize the sum of inner product
			sum = 0.0;
			// for each field in the input fields list
			for(String f : inputFields)
			{
				// take the text from that field
				String txt = s.getFieldString(f);;
				if (txt != null){
					// split the text into words
					String inputWords_toks[] = txt.split("\\s*[^a-zA-Z]+\\s*");
					// make a list of those words
					for(String w : inputWords_toks){
						// no dublicates
						word_counts.add(w);
					}
					
					for (String word : word_counts) {
						// if word already exists in the map
						if(wordsAndWeights.containsKey(word)==true){
							// increase the value by one
							sum = sum + 1;
						}
					}
				}
				
			}
			if(sum > Integer.parseInt(this.getProperty(PROPERTY_THRESHOLD)))
				//add the result to the proper field
				bb.addTagToDoc(s.getID(), outputTagID);
			
			//Data point was processed output report
			if(dataProcessed++%1000==0)	//Progress report every 1K docs
				System.out.printf("%.2f\n", dataProcessed *100.0 / MODULE_DATA_PROCESS_LIMIT );
			
			s = DocSet.getNext();
		}
		this.saveModuleResults(dataRead, dataProcessed);
		
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		BinaryRepresentation module = new BinaryRepresentation(args[0]);
		module.run();
	}

}
