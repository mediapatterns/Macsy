/**
 * Manages programs outputs.
 * All output is saved in predefined subfolder and filename in ./results
 * It stores outputs in buffer and print it out if QuiteMode is false.
 * It saves output to file on request.
 * 
 */
package macsy.lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Results {
	
	StringBuffer _output; 
	String _subfolder; 
	String _filename;
	boolean _QuiteMode = false;
	boolean bAppend = false;
	BufferedWriter fp = null;
	final static String _BASEPATH ="./";
	
	
	
	public Results(String res_subfolder, String filename,boolean QuiteMode) 
	{
		_subfolder = res_subfolder;
		_filename = filename;
		
		_output = new StringBuffer();
		
		_QuiteMode = QuiteMode;
		
		
		//CREATE DIRECTORY STRUCTURE IF NOT EXISTS
		File resultsdir = new File(_BASEPATH);
		if(!resultsdir.exists())
			resultsdir.mkdir();

		File resultssubdir = new File(_BASEPATH+"/"+_subfolder);
		if(!resultssubdir.exists())
			resultssubdir.mkdir();

	}
	
	public Results(String res_subfolder, 
			String filename,
			boolean QuiteMode,
			boolean Append) throws IOException 
	{
		_subfolder = res_subfolder;
		_filename = filename;
		
		_output = new StringBuffer();
		
		_QuiteMode = QuiteMode;
		
		bAppend = Append;
		
		//CREATE DIRECTORY STRUCTURE IF NOT EXISTS
		File resultsdir = new File(_BASEPATH);
		if(!resultsdir.exists())
			resultsdir.mkdir();

		File resultssubdir = new File(_BASEPATH+"/"+_subfolder);
		if(!resultssubdir.exists())
			resultssubdir.mkdir();
	}

	/**
//	 * Default: QuiteMode = false
//	 * @param res_subfolder
//	 * @param filename
//	 */
//	public Results(String res_subfolder,String filename) {
//		this(res_subfolder,filename,false);
//	}

	
	public void  print(String a) {
		printNwrite(a);
	}
	
	public void  println(String a) {
		printNwriteln(a);
	}
	
	
	public void  printNwrite(String a) {
		_output.append(a);
		if(!_QuiteMode)
			System.out.print(a);
	}

	public void  printNwriteln(String a) {
		_output.append(a+"\n");
		if(!_QuiteMode)
			System.out.println(a);
	}

	public void Flush() throws Exception
	{
		if(fp==null)
		{
			File resultssubdir = new File(_BASEPATH+"/"+_subfolder);			
			String resfilename=new String(resultssubdir.getPath()+"/"+_filename);	
			if(bAppend)
				fp = new BufferedWriter(new FileWriter(resfilename.toString(), true));
			else
				fp = new BufferedWriter(new FileWriter(resfilename.toString()));
		}
		
		fp.write(_output.toString());
		fp.flush();
		_output.delete(0,_output.length());
	}

	public void SaveOutput() {
		try{
			if(!_QuiteMode)
				System.out.print("Saving "+_filename + "...");
			
			if(fp == null)
			{
				File resultssubdir = new File(_BASEPATH+"/"+_subfolder);			
				String resfilename=new String(resultssubdir.getPath()+"/"+_filename);
				if(bAppend)
					fp = new BufferedWriter(new FileWriter(resfilename, true));
				else
					fp = new BufferedWriter(new FileWriter(resfilename));
			}
			
			fp.write(_output.toString());
			fp.close();
			
			if(!_QuiteMode)
					System.out.println("DONE");

		}
		catch(IOException e) {
			System.out.print("*** ERROR while saving: "+ e.toString());
		}
	}
}
