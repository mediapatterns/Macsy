package macsy.module.innerProductWithWeights;

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

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.module.BaseModule;

/**
 * Computes the weighted inner product of a text of interest (as an input vocabularry) and
 * documents in the database with a predefined Tag name and Field name, within a period of time.
 * The module writes the result on each document's registry.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELD=Run module only on docs that have this field.
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_VOCABULARY_FILENAME=The path and name of the file that stores a vocabulary with IDFs.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest.
 * INPUT_PRODUCT_COSINE=The user decides if he/she wants to calculate the inner product or the cosine
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 *  
 * Output:
 * OUTPUT_FIELD=The field's name where the module is going to write the inner product calculated.
 * OUTPUT_TAGS=The name of the tag that will be applied to all new inserted data.
 * INPUT_PRODUCT_COSINE=The user decides if he/she wants to calculate the inner product or the cosine.
 * 
 * @author Panagiota Antonakaki
 * Last Update: 15-10-2012
 */


public class InnerProductWithWeights extends BaseModule {
	// temporal variables to hold information given by the user
	static final String PROPERTY_INPUT_VOCABULARY_FILENAME = "INPUT_VOCABULARY_FILENAME";
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	static final String PROPERTY_INPUT_PRODUCT_COSINE = "INPUT_PRODUCT_COSINE";
			
	public InnerProductWithWeights(String propertiesFilename ) throws Exception 
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
		//check the decision of the user (inner product or cosine)
		String product_or_cosine = this.getProperty(PROPERTY_INPUT_PRODUCT_COSINE);
		boolean isFunctionInnerProduct = true;	//Variable name clarification
		if("COSINE".equals(product_or_cosine))	//inverse to avoid seg.fault if variable==null
			isFunctionInnerProduct = false;
		
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		//Prepare input tags
		String tagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> inputTagIDs = new LinkedList<Integer>(); 	//Variable name clarification + Java style
		for(String tagName : tagNames) 
		{
			if(!tagName.equals("")){
				int tagID = bb.getTagID(tagName);
				if( tagID==0){
					System.err.println("No known input tag");
					System.exit(0);
				}
				inputTagIDs.add(tagID);
			}
		}		
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
		
		// initialize the sum needed for the inner product
		double sum = 0.0;
		// create a map: <# of words in the doc, frequency>
		HashMap<String,Integer> word_counts = new HashMap<String,Integer>();
		while((s = DocSet.getNext())!=null){
			dataRead++;
			// if the list of the input tags is not empty and the document has all the input tags 
			if((inputTagIDs!=null) && (s.getTagIDs()!=null) && (s.getTagIDs().containsAll(inputTagIDs) ))
			{
				// initialize the sum of inner product
				sum = 0.0;
				dataProcessed++;
				// for each field in the input fields list
				for(String f : inputFields)
				{
					// take the text from that field
					String txt = s.getFieldString(f);;
					if (txt != null){
						// split the text into words
						List<String> inputWords = new ArrayList<String>();
						String inputWords_toks[] = txt.split("\\s*[^a-zA-Z]+\\s*");
						// make a list of those words
						for(String w : inputWords_toks){
							inputWords.add(w);
							// if the number of words already exists in the map
							if(word_counts.containsKey(w)==true){
								// increase the value by one
								int c = ((Integer)word_counts.get(w)).intValue();
								word_counts.put(w, c + 1);
							}
							else
								// put the new key - number of words
								word_counts.put(w, 1);
						}
						
						for (String word : inputWords) {
							// if the number of words already exists in the map
							if(wordsAndWeights.containsKey(word)==true){
								// increase the value by one
								sum = sum + (Double)wordsAndWeights.get(word).doubleValue();
							}
						}
					}
					
				}
			}			
			double res;
			
			if(isFunctionInnerProduct)
				res = sum;
			else
				res = sum/(word_counts.size()*wordsAndWeights.size());
			
			//add the result to the proper field
			bb.addFieldToDoc(s.getID(), MODULE_OUTPUT_FIELDS, res);
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
		//InnerProduct module = new InnerProduct("C:\\Users\\Γιώτα\\EclipseWorkspace\\ModACLE\\Projects\\TestModacleDB\\InnerProduct.settings");
		InnerProductWithWeights module = new InnerProductWithWeights("C:\\Users\\Γιώτα\\EclipseWorkspace\\Macsy\\Projects\\TestGiota\\WeightedInnerProduct.settings");
		//InnerProduct module = new InnerProduct(args[0]);
		module.run();
	}
}
