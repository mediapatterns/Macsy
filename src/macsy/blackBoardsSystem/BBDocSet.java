package macsy.blackBoardsSystem;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This class is used to store a set of docs.
 * 
 * Functions in Black Boards that return set of docs, use this class as output. 
 *   
 * @author Ilias Flaounas, Tom Welfare
 * @version     1.1
 * @since  2014-03-12
 *
 */
public class BBDocSet {

	private final int MONGO_DB_BATCH_SIZE = 50;	//Reduce number if cursor timeout events occur. 
	
	/**
	 * A list of cursor results. 
	 */
	List<DBCursor> cursors = null;

	/**
	 * A pointer to the cursor from witch the next article will be returned.
	 */
	int currentCursor = 0; 

	/**
	 * Creates a new BBDocArticleSet populated with cursor result.
	 * @param cursor 
	 */
	BBDocSet(DBCursor cursor)
	{
		this.cursors =  new ArrayList<DBCursor>();

		if(cursor!=null) {
			cursor.batchSize(MONGO_DB_BATCH_SIZE);
			cursors.add(cursor);
		}

		currentCursor = 0;
	}

	BBDocSet(BBDocSet clone)
	{
		this.cursors =  clone.cursors;
		this.currentCursor = clone.currentCursor;
	}
	
	
	/**
	 * Creates a new BBDocArticleSet populated with all cursor results.
	 * @param cursors A list of cursors
	 */
	BBDocSet(List<DBCursor> cursors)
	{
		this.cursors = cursors;
		for(DBCursor c : this.cursors) {
			c.batchSize(MONGO_DB_BATCH_SIZE);
		}
		
		currentCursor = 0;
	}

//	private static BBDocSet getInstance(List<DBCursor> cursors)
//	{
//		return new BBDocSet(cursors);
//	}
//	
//	public static BBDocSet getInstance(DBCursor cursor)
//	{
//		return new BBDocSet(cursor);
//	}
//	
	
	/**
	 * Each time it is called it returns the next doc, or null if no more docs are found. 
	 *  
	 * @return The next doc as BBDoc or null if next doc doesn't exist. 
	 */
	public BBDoc getNext()
	{
		while(currentCursor < cursors.size())
		{
			if(cursors.get(currentCursor).hasNext())
			{
				DBObject art = cursors.get(currentCursor).next();
				return new BBDoc(art);
			}
			currentCursor ++;
		}
		return null;
	}

	public BBDocSet clone()
	{
		List<DBCursor> cursors_copy = new ArrayList<DBCursor>();
		for(DBCursor c: cursors) {
			cursors_copy.add( c.copy() );
			c.batchSize(MONGO_DB_BATCH_SIZE);
		}
		
		return new BBDocSet(cursors_copy);
	}
}
