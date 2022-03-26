package heapdb;

import static heapdb.Constants.BLOCK_SIZE;

import java.nio.ByteBuffer;

public class LSMindex {
	
	/*
	 * @author Matthew Fernandez 
	 */

	private BlockedFile bfile;
	private ByteBuffer rootBuffer;
	private int keyType;
	private ByteBuffer buildBuffer;
	
	/**
	 *  Open file for LSM index
	 * @param filename
	 * @param table schema  
	 * @param create  true -- create new index,  false -- open existing index
	 */
	public LSMindex(String filename, Schema schema, boolean create ) {
		keyType = schema.getType(schema.getKey());
		bfile = new BlockedFile(filename, create);
		rootBuffer = ByteBuffer.wrap(new byte[heapdb.Constants.BLOCK_SIZE]);
		buildBuffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
		buildBuffer.position(8);
		
		// read and cache the root index node
		if (!create) {
			bfile.readBlock(bfile.getHighestBlockNo(), rootBuffer);
		}
	}
	
	public void close() {
		bfile.close();
	}

	/**
	 * Given key, return blockNo value for key in index.
	 * Index is a sparse index. 
	 */
	public int lookup(Object key) {
		// TODO  complete this method
		//   you may use "rootBuffer" as root node buffer.
		//     The root node is read and kept in memory by the LSMindex constructor method.
		//   use method "lookupInBlock" to search a buffer for a key value.
		//    set buffer position to 8 before calling "lookupInBuffer"
		rootBuffer.position(8);
		int blockno = lookupInBlock(rootBuffer , key);
		while(blockno<=0 && blockno != Integer.MIN_VALUE) {
			ByteBuffer b = ByteBuffer.wrap(new  byte[heapdb.Constants.BLOCK_SIZE]);
			bfile.readBlock(-blockno, b);
			blockno =lookupInBlock(buildBuffer, key);
			blockno =lookupInBlock(buildBuffer, key);  // should be lookupInBlock(b, key).
		}
		return blockno;
	}
	
	/**
	 * insert new entry (key, blockno) into index leaf block
	 * @param key is a tuple key value
	 * @param blockno is block number of data file
	 */
	public void insertEntry(Object key, int blockno) {
		// TODO complete this method
		// hints:
		// use buildBuffer as the ByteBuffer.
		// study and use methods 
		//      clearBuffer -- this method is defined in this class.
		//      Tuple.serializeKey -- convert a key value to a byte[].  You can then determine the key size in bytes. 
		//      BlockedFile.appendBlock -- write buffer to the end of the file.
		//      ByteBuffer.putInt --  write a int value  to ByteBuffer
		//      ByteBuffer.put -- write a byte[] to ByteBuffer
		byte b [] = Tuple.serializeKey(key);
		if(buildBuffer.remaining() >= b.length+4) {  
			buildBuffer.put(b);   // if space remains in current buffer
			buildBuffer.putInt(blockno);
		} else {
			bfile.appendBlock(buildBuffer);
			clearBuffer(buildBuffer); // put b and blockno into an empty buffer
			buildBuffer.put(b);
			buildBuffer.putInt(blockno);
		}
	}
	
	/**
	 * create non-leaf nodes of index tree.  Root node must be the last block in the index file.
	 */
	public void createIndexTree() {
		// TODO complete this method
		// hints:
		// study and use methods 
		//      clearBuffer -- this method is defined in this class.
		//      getKey -- return the first key value in an index block
		//      BlockedFile.appendBlock -- write buffer to the end of the file.
		//      insertEntry -- see above method
		if (buildBuffer.position() > 8) {
			bfile.appendBlock(buildBuffer);
			clearBuffer(buildBuffer);
		}
		// build parent and higher index levels up to the root.
		int lo=0;
		int hi=bfile.getHighestBlockNo();
		ByteBuffer buffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
		while (lo < hi) {
			for (int k=lo; k<=hi; k++) {
				bfile.readBlock(k, buffer);
				insertEntry(getKey(buffer), -k);
			}
			// write out any partially filled buffer.
			if (buildBuffer.position() > 8) {
				bfile.appendBlock(buildBuffer);
				clearBuffer(buildBuffer);
			}
			lo=hi+1;
			hi=bfile.getHighestBlockNo();
		}
		// reset root block
		bfile.readBlock(bfile.getHighestBlockNo(), rootBuffer);
	}

	private void clearBuffer(ByteBuffer buffer) {
		byte[] node_bytes = buffer.array(); // clear bytes
		for (int i = 0; i < heapdb.Constants.BLOCK_SIZE; i++) {
			node_bytes[i] = 0;
		}
		buffer.position(8);
	}

	/**
	 * return next key value from buffer
	 * @param buffer 
	 */
	private Object getKey(ByteBuffer buffer) {
		Object key = null;
		switch (keyType) {
		case heapdb.Constants.INT_TYPE:
			key = buffer.getInt();
			break;
		case heapdb.Constants.VARCHAR_TYPE:
			byte[] bytes = new byte[buffer.getInt()];
			buffer.get(bytes);
			key = new String(bytes);
			break;
		default:
			throw new RuntimeException("Invalid key type. " + keyType);
		}
		return key;
	}

	/**
	 * search index block for key value
	 * @param buffer to search
	 * @param key value to search for
	 */
	private int lookupInBlock(ByteBuffer buffer, Object key) {
		int priorBlockNo = Integer.MIN_VALUE;
		while (buffer.remaining() > 0) {
			Object v;
			if (key instanceof Integer) {
				v = buffer.getInt();
			} else if (key instanceof String) {
				byte[] bytes = new byte[buffer.getInt()];
				buffer.get(bytes);
				v = new String(bytes);
			} else
				throw new RuntimeException("Invalid key type. " + key.getClass() + " " + key);

			int c = Tuple.compareKeys(key, v);
			if (c < 0) {
				return priorBlockNo;
			} else if (c == 0) {
				return buffer.getInt();
			} else {
				priorBlockNo = buffer.getInt();
			}
		}
		return priorBlockNo;
	}

	/**
	 * for debugging. This method will print all index entries. 
	 */
	public void printDiagnostic() {
		int hblockno = bfile.getHighestBlockNo();
		if (hblockno<0) {
			System.out.printf("Index empty");
			return;
		} else {
			System.out.printf("Index high block %d\n", hblockno);
		}

		ByteBuffer buffer = ByteBuffer.wrap(new byte[heapdb.Constants.BLOCK_SIZE]);

		for (int blockNo=0; blockNo <= bfile.getHighestBlockNo(); blockNo++) {
			bfile.readBlock(blockNo, buffer);
			System.out.printf("Index block %d used bytes %d\n", blockNo, buffer.getInt(0));
			buffer.limit(buffer.getInt(0));
			buffer.position(8);
			int recno = 0;

			while (buffer.remaining() > 0) {
				Object key = getKey(buffer);
				int kblockno = buffer.getInt();
				System.out.printf(" Index Rec %d [%s %d]\n", recno, key.toString(), kblockno);
				recno++;
			}
		}
	}

}
