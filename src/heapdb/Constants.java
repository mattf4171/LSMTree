package heapdb;

public interface Constants {
	
	// file constants
	public static final int BLOCK_SIZE = 4096;
	public static final int SCHEMA_BLOCK = 0; // index of block containing schema
	
	// schema constants
	public  static final int MAX_COLUMN_NAME_LENGTH = 24;
	public  static final int INT_TYPE = 1;
	public  static final int VARCHAR_TYPE = 2;
	
	// LSM memory tree limit
	public static final int LIMIT_0 = 10;
	
}
