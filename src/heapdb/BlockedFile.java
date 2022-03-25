package heapdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import static heapdb.Constants.BLOCK_SIZE;

/**
 * Provide random read and write block operations for a file.
 *
 */
public class BlockedFile  {
	
	protected RandomAccessFile file; 
	protected int highBlockNo;
	
	/*
	 * open a file.  
	 * If file exists it is first deleted a new empty file is created.
	 * If file does not exist, a new empty file is created.
	 */
	BlockedFile(String filename, boolean create) {
		if (create) {
			File f = new File(filename);
			boolean b = f.exists();
			if (b) {
				b=f.delete();
				if (!b) {
					throw new RuntimeException("Unable to create file.  Cannot delete old file. "+filename);
				}
			}
			try {
				file = new RandomAccessFile(filename, "rw");
				highBlockNo =   -1;
			} catch (Exception e) {
				throw new RuntimeException( e.getMessage() );
			}
		} else {
			try {
				file = new RandomAccessFile(filename, "rw");
				highBlockNo =   (int)(file.length()/heapdb.Constants.BLOCK_SIZE) -1 ;
			} catch (Exception e) {
				throw new RuntimeException( e.getMessage() );
			}
		}
	}
	
	/*
	 * return the highest block number for the file.
	 * The first block in the file is block 0. 
	 * If the file is empty, the highest block number will be -1. 
	 */
	int getHighestBlockNo() { 
		return highBlockNo;
	}
	

	/*
	 * read a block from the file byte buffer.
	 */
	void readBlock(int blockNo, ByteBuffer buffer) {
		int nobytes = 0;
		try {
			file.seek(blockNo*BLOCK_SIZE);
			nobytes = file.read(buffer.array());
			buffer.limit(buffer.getInt(0));
			buffer.position(8);
		} catch (Exception e) {
			throw new RuntimeException( e.getMessage() );
		}
		if (nobytes != BLOCK_SIZE) {
			throw new RuntimeException("Error: BlockedFile read returned truncated block "+blockNo+" "+nobytes);
		}
	}
	
	/* 
	 * close the file.
	 */
	void close() {
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * write a block from the byte buffer.
	 */
	void writeBlock(int blockNo, ByteBuffer buffer) {
		try {
			if (blockNo > highBlockNo) {
				highBlockNo = blockNo;
			}
			file.seek(blockNo*BLOCK_SIZE);
			buffer.putInt(0,buffer.position());
			file.write(buffer.array());
		} catch (Exception e) {
			throw new RuntimeException( e.getMessage() );
		}
	}
	
	/*
	 * write block to end of file.
	 */
	void appendBlock(ByteBuffer buffer) {
		highBlockNo++;
		writeBlock(highBlockNo, buffer);
	}
}
