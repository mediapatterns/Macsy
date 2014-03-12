package macsy.module.featuresExtractorTFIDF;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoard;


public class FeaturesExtractorTFIDF_DAO {
	
	
//	public static final String TITLE_DESC = "TD";
//	public static final String TITLE_DESC_CONTENT = "TDC";
//	public static final String CONTENT = "C";

//	//INPUT
//	private	String 	INPUT_TAG;
//	private int 	TAG_FOR_INDEXER_ID;
//
//	//OUTPUT
//	private String 	TAG_POST_INDEXER;
//	private int 	TAG_POST_INDEXER_ID;
//	private String 	FIELD_INDEXER; 
	
	BlackBoard bb; 
	

	public FeaturesExtractorTFIDF_DAO(BlackBoard bb  ) throws Exception
	{
		this.bb = bb;
	
//		TAG_FOR_INDEXER = ForIndexerTag;
//		TAG_POST_INDEXER = PostIndexerTag;
//		FIELD_INDEXER = ReadabilityField;
//		
//		TAG_FOR_INDEXER_ID = getTagID(TAG_FOR_INDEXER);
//		TAG_POST_INDEXER_ID = getTagID(TAG_POST_INDEXER);
	}

	
	public BBDocSet getInputDocs(int inputTagID, int limit) throws Exception
	{
		LinkedList<Integer> requiredTags = new LinkedList<Integer>();
		requiredTags.add( inputTagID  );
	
//		GregorianCalendar today = new GregorianCalendar();
//		Date fromDate = new GregorianCalendar(today.get(Calendar.YEAR),0,1,0,0).getTime();
//		Date toDate = today.getTime();
//		
		
		BBDocSet articles = bb.findDocsByTagsSet(
				requiredTags,
				null,
				limit);

		return articles;
	}
	
	
	public String getDocText(Object docID, List<String> inputFields) throws Exception
	{
		BBDoc doc = bb.findDocByID(docID);
		StringBuilder artText = new StringBuilder(); 
		
		for(String field : inputFields)
		{
			String text = (String) doc.getField( field);
			if(text!=null)
				artText.append(  text + ". " );
		}
				
		return artText.toString();
	}
	
	public void storeIndexed(Object docID, String outputField, Map<Integer,Double> key_val) throws Exception
	{
		List<Double> keyval_list = new LinkedList<Double>();
		
		for (Map.Entry<Integer, Double> e : key_val.entrySet()) 
		{
			keyval_list.add((double)e.getKey());
			keyval_list.add(e.getValue());
		}
		
		bb.addFieldToDoc(docID, outputField, keyval_list);
	}

	
	public int getTagID(String tagName) throws Exception
	{
		int tagID =  bb.getTagID(tagName);
		if(tagID == BlackBoard.TAG_NOT_FOUND)
			tagID = bb.insertNewTag( tagName );
		return tagID;
	}
//	
	public void addTagToDoc(Object articleID, int tagID) throws Exception
	{
		bb.addTagToDoc(articleID, tagID);
	}

	public void removeTagFromDoc(Object articleID, int tagID) throws Exception
	{
		bb.removeTagFromDoc(articleID, tagID);
	}

	
}
