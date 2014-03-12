package macsy.input.importTextFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.module.BaseModule;

/**
 * Imports a set of text files into a blackboard.
 * 
 * Input:
 * INPUT_FILES=The path of a folder with text files to be used as input. Files in all subfolders will be added.
 * 
 * Output:
 * OUTPUT_BLACKBOARD=The name of the BlackBoard that will be populated by docs.
 * OUTPUT_TAGS=The name of a single tag that will be applied to all new inserted docs.
 * OUTPUT_FIELDS=The name of the field that will store the content of the text file.
 * 
 * @author Ilias Flaounas
 * @since 1/11/2012
 *
 */
public class ImportTextFiles extends BaseModule {

	static final String PROPERTY_INPUT_FILES = "INPUT_FILES";
	static final String FIELD_PATHNAME = "Path";
	static final String FIELD_FILENAME = "Filename";

	private  BlackBoard outputBB = null;
	List<Integer> outputTagIDs = null;
	int docsWritten = 0;
	int filesRead = 0;

	private static String SEPARATOR;
	{
		SEPARATOR = System.getProperty("file.separator");
	}

	public ImportTextFiles(String propertiesFilename ) throws Exception 
	{
		super(propertiesFilename);
	}



	public void runModuleCore() throws Exception 
	{
		if(MODULE_OUTPUT_FIELDS==null)
			throw new Exception("You have to set the OUTPUT_FIELDS in settings file.");
		if(MODULE_OUTPUT_BLACKBOARD==null)
			throw new Exception("You have to set the OUTPUT_BLACKBOARD in settings file.");

		//Load Black Board of interest
		outputBB = _bbAPI.blackBoardLoad(  MODULE_OUTPUT_BLACKBOARD );

		//Prepare output TAGs
		outputTagIDs = new LinkedList<Integer>();
		if(MODULE_OUTPUT_TAGS!=null)
		{
			String tagNames[] = MODULE_OUTPUT_TAGS.split(",");
			for(String tagName : tagNames) 
			{
				int tagID = outputBB.getTagID(tagName);
				if( tagID==0)
					tagID = outputBB.insertNewTag(tagName);
				outputTagIDs.add(tagID);
			}
		}

		String txtFilename = this.getProperty(PROPERTY_INPUT_FILES);
		readPath( txtFilename );

		this.saveModuleResults(filesRead, docsWritten);
	}


	public void readPath(String startingPath) throws Exception
	{
		File path = new File( startingPath );

		String[] filenames = path.list();
		for(String filename : filenames)
		{
			File f = new File( filename );
			if( f.isDirectory() )
				readPath( startingPath + SEPARATOR + filename );
			else
				readFileAndStoreDoc( filename, startingPath );
		}

	}

	public void readFileAndStoreDoc(String filename, String pathname ) throws Exception
	{		
		filesRead++;

		BufferedReader input = new BufferedReader( new FileReader(pathname +SEPARATOR+ filename) );

//		int linesRead = 0;

		StringBuilder txt = new StringBuilder();

		String line = null; 
		while (( line = input.readLine()) != null)
		{
//			linesRead++;
			txt.append( line + "\n");
		}
		input.close();

		BBDoc doc = new BBDoc();
		doc.setField(MODULE_OUTPUT_FIELDS, txt.toString() );
		doc.setField(FIELD_PATHNAME, pathname );
		doc.setField(FIELD_FILENAME, filename);
		doc.setTags( outputTagIDs );
		outputBB.insertNewDoc( doc );
		docsWritten++;

		//Print out max 100 chars per document.
		int maxL = txt.toString().length();
		if( maxL > 100 )		
			maxL = 100;
		System.out.println(txt.toString().substring(0,maxL) );
		System.out.println(pathname);
		System.out.println(filename);

	}


	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		ImportTextFiles module = new ImportTextFiles(args[0]);
		module.run();
	}


}
