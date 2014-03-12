package macsy.module.onLineLearningWinnowOnWords;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import macsy.blackBoardsSystem.*;
import macsy.lib.WinnowVocabularyOnWords;
import macsy.module.BaseModule;

/**
 * Implements online learning using Winnow algorithm.
 * It adjust the weight vector w according to the INPUT_LEARN_TAGS, 
 * and the learning information is printed on the screen but also on the txt file STATISTICAL. 
 * It also updates the documents in the database by adding: 
 * a) a new tag to all processed docs (positive or negative according to the predicted output).
 * b) a field with y_hat value.
 * 
 * y_hat(t) = <w(t).x(t)>
 * 
 * The module works  on every document that carries all the tags in INPUT_TAG field.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that contains the documents of interest.
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_FIELD=The field that we want to apply the online learning of the docs.
 * INPUT_TXT_FILENAME=The name of the file that modules saves the w vector.
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * INPUT_LEARN_TAGs=The tags on which the learning is based (one tag for each class)
 * STATISTICAL=The txt filename for the module to write all statistical results
 * 
 * Output:
 * OUTPUT_FIELDs=The fields' names where the module is going to write the predicted value y_hat.
 * OUTPUT_TAGS=The name of the tags that will be applied to all processed docs (one tag for 
 * the positive class and one for the negative one).   
 * 
 * @author Panagiota Antonakaki
 * Last Update: 12-11-2012
 * 
 */
public class OnLineLearningWinnowOnWords extends BaseModule {
	// temporal variables to hold information given by the user
	static final String PROPERTY_TXT_FILENAME = "TXT_FILENAME";
	static final String PROPERTY_LEARNING_TAGS = "INPUT_LEARN_TAGS";
	static final String PROPERTY_STATISTICAL = "STATISTICAL";
	
	// Define the positions of positive (0) and negative (1) tags in the tag list
	int POSITIVE_TAG_INDEX = 0;
	int NEGATIVE_TAG_INDEX = 1;
	
	
	//The w / vocabulary is saved here.
	private WinnowVocabularyOnWords w_voc  = null;
	
	public OnLineLearningWinnowOnWords(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	// the information we need: a map with the IDs of the words used in the text and their TFs values
	// 							and the correct output y
	private class Sample {
		public Map<Integer,Double> features;
		public double y;
		public Sample(Map<Integer,Double> f, double y)
		{
			this.features = f;
			this.y=y;
		}
	}
	
	/**
	 * Transforms the input features to a features Map based on TF.
	 * 
	 * @param featuresAsString the string text that you wand to transform into a Map based on TF
	 * @return the map  of the words' IDs and their TF
	 * @throws InterruptedException
	 */
	private Map<Integer,Double> Features_WordID(String featuresAsString) throws Exception
	{
		String[] terms = featuresAsString.split(" ");
		
		Map<Integer,Double> feat = new TreeMap<Integer,Double>();
		
		for(int t=0;t<terms.length;t++) 
		{	
			//Skip special chars
			if((terms[t].equals("*")) || (terms[t].equals("") ) )
					continue;

			int id = w_voc.getIDofWord(terms[t]);
			if(id==0) 	{//Add if is a new word
				try{
						id = w_voc.addWord( terms[t] );
				}
				catch(Exception e){
					System.out.println("Can not add new word to voc");}
			}
			if(feat.containsKey(id))
			{
				double old_value = feat.get(id);
				feat.put(id, old_value +1 );
			}
			else
				feat.put(id, 1.0);
		}
		return feat;
	}
	
	/**
	 * Stores the correct output according to the input learning tags.
	 * 
	 * @param s : the document being processed.
	 * @param inputLearnTagIDs : the list of the input learning tags
	 * @param X_i : the input feature vector
	 * @param input_X : the list of the feature vectors and their correct 
	 * 					outputS (POSITIVE_TAG_INDEX or NEGATIVE_TAG_INDEX)
	 */
	void CorrectOutput(BBDoc s, List<Integer> inputLearnTagIDs, Map<Integer,Double> X_i, List<Sample> input_X){
		if(!s.getAllTagIDs().contains(inputLearnTagIDs.get(POSITIVE_TAG_INDEX)) &&
				(s.getAllTagIDs().contains(inputLearnTagIDs.get(NEGATIVE_TAG_INDEX))))
			input_X.add(new Sample(X_i, 0.0));
		// decide which one of the tags it appears
		else if(s.getAllTagIDs().contains(inputLearnTagIDs.get(POSITIVE_TAG_INDEX)))
			input_X.add(new Sample(X_i, 1.0));
		else
			input_X.add(new Sample(X_i, -1.0));
	}
	
	/**
	 * Calculates the Inner Product of the frequencies of the words in the text (TFs) and their
	 * weights (in the w_doc)
	 * 
	 * @param w_doc the vocabulary with the weights
	 * @param x the input (map of IDs and TFs)
	 * @return the predicted output according to the input x
	 */
	private double calculateDotExtendedWX(WinnowVocabularyOnWords w_voc, Map<Integer,Double> x) throws InterruptedException
	{
		double weight = 0;
		for(Map.Entry<Integer,Double> e : x.entrySet())
			weight += w_voc.getValueByID( e.getKey() ) * e.getValue();
		weight += w_voc.getValueByID(0);
		
		return weight;
	}
	
	@Override
	public void runModuleCore() throws Exception 
	{
		// check whether you also want txt output file
		StringBuilder outputBuffer = null;
		String txtFilename = this.getProperty(PROPERTY_STATISTICAL);
		if(txtFilename!=null)
		{
			outputBuffer = new StringBuilder();
		}
				
		// initialize the weights (if the model is not already saved make one with seros)
		w_voc  = new WinnowVocabularyOnWords(this.getProperty(PROPERTY_TXT_FILENAME));
		
		// load the black board
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		//Prepare input tags 
		String inputTagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> inputTagIDs = new LinkedList<Integer>();	//Variable name clarification + Java style
		for(String tagName : inputTagNames) 
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
		
		//Prepare tags for learning 
		String inputLearnTagNames[] = this.getProperty(PROPERTY_LEARNING_TAGS).split(",");
		List<Integer> inputLearnTagIDs = new LinkedList<Integer>();	
		for(String tagName : inputLearnTagNames) 
		{
			if(!tagName.equals("")){
				int tagID = bb.getTagID(tagName);
				if( tagID==0){
					System.err.println("No known input tag");
					System.exit(0);
				}
				inputLearnTagIDs.add(tagID);
			}
		}
		
		//Input Fields
		List<String> inputFields = new ArrayList<String>();
		String inFields_toks[] = MODULE_INPUT_FIELDS.split(",");
		for(String f : inFields_toks)
			inputFields.add(f);
		
		//Prepare output tags 
		String outTagNames[] = MODULE_OUTPUT_TAGS.split(",");
		List<Integer> outputTagIDs = new LinkedList<Integer>();//Variable name clarification
		for(String tagName : outTagNames) 
		{
			if(!tagName.equals("")){
				int	outputTagID = bb.getTagID(tagName);
				// if the tag not already exists insert new tag 
				if( outputTagID==0)
					outputTagID=bb.insertNewTag(tagName);
				outputTagIDs.add(outputTagID);
			}
		}
		
		//output Field
		String outputField = MODULE_OUTPUT_FIELDS;
		
		//Find the documents in the BB that have that input tag (subset)
		BBDocSet DocSet = bb.findDocsByTagsSet(inputTagIDs, null, this.MODULE_DATA_PROCESS_LIMIT);

		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;

		List<Sample> input_X = new LinkedList<Sample>();
		
		int doc_index = 0;
		int iteration = 1;

		long TP=0,TN=0,FP=0,FN=0;
		double Precision = 0.0, Recall = 0.0, F_measure = 0;
		
		// statistical information
		String str;
		
		str = "Iteration \t Precision \t Recall \t F-measure \t Errors \n  ";
		
		System.out.println(str);
		if(outputBuffer!=null)
			outputBuffer.append(str);
		
		// as long as there are still docs in the BBDocSet
		// get the next doc int the BBDocSet
		BBDoc s;
		while((s = DocSet.getNext())!=null)
		{  
			int index_of_errors = 0;
			dataRead++;
			String txt = null;
			// for each field in the input fields list
			for(String f : inputFields)
			{
				// take the text from that field
				String field_txt = s.getFieldString(f);
				if (field_txt != null){
					// concatenate the texts
					txt = txt + field_txt;
				}
			}
			
			// Representation - Feature Extraction
			Map<Integer,Double> X_i = Features_WordID(txt);
			CorrectOutput(s, inputLearnTagIDs, X_i, input_X);
			double y_hat = this.calculateDotExtendedWX(w_voc, input_X.get(doc_index).features);
			
			//TODO: Check threshold!
			if (y_hat >= 200)
				y_hat = +1;
			else
				y_hat = -1;
			// Learning
										
			// if the tags of the document contains the tags of interest for learning
			if( (inputLearnTagIDs!=null) && (s.getTagIDs()!=null) && 
					( s.getTagIDs().contains(inputLearnTagIDs.get(POSITIVE_TAG_INDEX) ) ) || 
					( s.getTagIDs().contains(inputLearnTagIDs.get(1) ) ) )
			{
				dataProcessed ++;
				// find the y output
				CorrectOutput(s, inputLearnTagIDs, X_i, input_X);
				
				// take the desired and the predicted output
				double y = input_X.get(doc_index).y;
				//double y_hat = this.calculateDotExtendedWX(w_voc, input_X.get(doc_index).features);
				
				// if they are different you have an error
				boolean error = false;
				if(y_hat>=0)
				{
					System.out.println("positive");
					System.out.println(outTagNames[POSITIVE_TAG_INDEX] + " : " + 
					outputTagIDs.get(POSITIVE_TAG_INDEX));
					System.out.println("y_hat : " + y_hat);
					// adds a Tag and the calculated predicted value in the document
					bb.addTagsToDoc(s.getID(), outputTagIDs);
					bb.addFieldToDoc(s.getID(), outputField, y_hat);
					if(y>=0)
						TP++;
					else
					{
						FP++;
						index_of_errors++;
						error = true;
					}
				}
				else
				{
					System.out.println("negative");
					System.out.println(outTagNames[NEGATIVE_TAG_INDEX] + " : " + 
					outputTagIDs.get(NEGATIVE_TAG_INDEX));
					System.out.println("y_hat : " + y_hat);
					// adds a Tag and the calculated predicted value in the document
					//bb.addTagsToDoc(s.getID(), outputTagIDs);
					//bb.addFieldToDoc(s.getID(), outputField, y_hat);
					if(y<0)
						TN++;
					else
					{
						FN++;
						index_of_errors++;
						error = true;
					}
						
				}
				// if you have an error you need to adjust weight vector
				if(error){
					Sample Xj = input_X.get(doc_index);
					double Yjt = calculateDotExtendedWX( w_voc, Xj.features);
						
					Yjt = (Yjt>0 ? 1.0 : -1.0);	//Threshold
						
					if(Yjt<0 && y>0)
					{
						for(Map.Entry<Integer,Double> e : Xj.features.entrySet())
						{
							double w_prev = w_voc.getValueByID(  e.getKey()  );
							if(w_prev == 1)
								w_voc.setValueByID( e.getKey(), 2*e.getValue() );
						}
					}
					else if(Yjt>=0 && y<0)
					{
						for(Map.Entry<Integer,Double> e : Xj.features.entrySet())
						{
							double w_prev = w_voc.getValueByID(  e.getKey()  );
							if(w_prev == 1)
								w_voc.setValueByID( e.getKey(), e.getValue()/2 );
						}
					}
					
					//Constant
					w_voc.setValueByID(0, 1.0 );
				}
				if(TP + FP !=0)
					Precision = (double)TP/(double)(TP + FP);
				else
					Precision = 0.0;
				if(TP + FN != 0)
					Recall = (double)TP/(double)(TP + FN);
				else
					Recall = 0.0;
				if(Precision + Recall != 0)
					F_measure = 2*Precision*Recall/(Precision + Recall);
				else
					F_measure = 0.0;
						
				// statistical information
				str = iteration + " \t ";
				str = str + Precision + " \t " + Recall + " \t " + 
				F_measure + " \t ";
				str = str + index_of_errors + " \n ";
				
				
				System.out.print(str);
				if(outputBuffer!=null)
					outputBuffer.append(str);
				
				
				/*str = "\n\t\tActual class\n\t\tP\tN\nPred.P\t" + TP + "\t" + FP;
				str = str + "\nPred.P\t" + FN + "\t" +TN;
				System.out.println(str);
				if(outputBuffer!=null)
					outputBuffer.append(str);*/
				System.out.println("\t\tActual class\n\t\tP\tN\nPred.P\t" + TP + "\t" + FP);
				System.out.println("Pred.N\t" + FN + "\t" +TN);
				
				
				iteration++;
				
				//if(	(errors_prev <= index_of_errors)  && (iteration>200) )
					//break;
			}
			else
				input_X.add(new Sample(X_i, 0.0));
			doc_index++;
		}
		System.out.println("DONE");//Training
		
		if(outputBuffer!=null)
		{
			File resultssubdir = new File( txtFilename );			
			BufferedWriter fp = new BufferedWriter(new FileWriter(resultssubdir));
			fp.write( outputBuffer.toString() );
			fp.close();
		}
		
		//Save new w
		System.out.print("Saving voc...");
		//System.out.println(w_voc.m_Id2Value.size());
		w_voc.saveVocabulary();
		System.out.println("DONE");
		
		// display the number of input items and the number of output items
		this.saveModuleResults(dataRead, dataProcessed);
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		
		OnLineLearningWinnowOnWords module = new OnLineLearningWinnowOnWords(args[0]);
		
		module.run();
	}
}

