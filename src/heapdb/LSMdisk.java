package heapdb;

import static heapdb.Constants.BLOCK_SIZE;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.TreeMap;

public class LSMdisk implements Iterable<Tuple> {
	
	/*
	 * @author Matthew Fernandez 
	 */
	
	private Schema schema;
	private BlockedFile bfile;
	private String filename;
	private LSMindex index;

	/**
	 * Create new file and write schema to block 0.
	 */
	public LSMdisk(String filename, Schema schema) {
		this.schema=schema;
		this.filename = filename;
		ByteBuffer block0  = ByteBuffer.wrap(new  byte[heapdb.Constants.BLOCK_SIZE]);
		block0.position(8);
		bfile = new BlockedFile(filename+".lsm", true);
		schema.serialize(block0);
		bfile.writeBlock(0,block0);
		index = new LSMindex(filename+".ism", schema, true);  // create new empty index file
	}
	
	/**
	 * Open existing file and read schema from block 0.
	 */
	public LSMdisk(String filename) {
		bfile = new BlockedFile(filename+".lsm", false);
		ByteBuffer block0  = ByteBuffer.wrap(new  byte[heapdb.Constants.BLOCK_SIZE]);
		bfile.readBlock(0,block0);
		this.schema = Schema.deserialize(block0);
		index = new LSMindex(filename+".ism", schema, false);  // open existing index file
	}
	
	public Schema getSchema() {
		return schema;
	}
	
	public int getHighestBlockNo() {
		return bfile.getHighestBlockNo();
	}
	
	public void close() {
		bfile.close();
		index.close();

	}
	
	/**
	 * lookup key in level 1 file.
	 */

	public Tuple lookup(Object key)  {
		/* 
		 * use LSMiterator to read all tuples in file and search for key. 
		 */
		ByteBuffer byte_buffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
		int blockno = index.lookup(key);
	        if (blockno<=0) {
	            return null;
	        } else {
	            byte_buffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
	            bfile.readBlock(blockno,  byte_buffer);
	            //  TODO code a loop to scan tuples in disk block look for tuple with the give key value.  ~ 5-10 lines of code.
	            while(byte_buffer.position() < byte_buffer.limit()) {
	            	Tuple head = Tuple.deserialize(schema, byte_buffer);
//	            	if(byte_buffer.remaining() != 0) {
            		if(head.getKey().equals(key)) {
	    				return head;
	    			}
	            }
	         return null;
	         }
		}
	
	
	/**
	 * merge data in level0 memory TreeMap 
	 * with existing level 1 data file. 
	 * Write out merged to data to a temporary file.
	 * Then delete data file and rename temporary file to data file.
	 */
     boolean merge(TreeMap<Object, Tuple> level0) {
		
    	 // create LSMdisk file to write to
		LSMdisk tempFile = new LSMdisk(filename+".temp", schema);
		LSMWriter bw = tempFile.getWriter();
	
		// get iterators over TreeMap and current LSMdisk file
		Iterator<Entry<Object, Tuple>> it0 = level0.entrySet().iterator();
		Iterator<Tuple> it1 = this.iterator();
		
		/**
		 * to get the next Tuple from iterator it0, use the code it0.next().getValue()
		 * to get the next Tuple from iterator it1, use the code it1.next()
		 * remember to check hasNext() before calling next. 
		 * 
		 * to compare key,  use Tuple.compareKeys method.
		 * 
		 * write tuples in key sequence to new file using LSMWriter.
		 */

		Tuple headA, headB; // merging two sorted lists -2 slide 
        headA = (it0.hasNext()) ? it0.next().getValue() : null;
        headB = (it1.hasNext()) ? it1.next() : null;

        while (headA != null && headB != null) { 
            if (Tuple.compareKeys(headA.getKey(), headB.getKey()) <= 0) {
                if (!(headA instanceof TupleDeleted)) { // not a tombstone
                    bw.append(headA);
                }
                headA = (it0.hasNext()) ? it0.next().getValue() : null;
            } else { // has a tombstone
                bw.append(headB);
                headB = (it1.hasNext()) ? it1.next() : null;
            }
        }

        while (headA != null) {
            if (!(headA instanceof TupleDeleted)) {
                bw.append(headA);
            }
            headA = (it0.hasNext()) ? it0.next().getValue() : null;
        }
        while (headB != null) {
            bw.append(headB);
            headB = (it1.hasNext()) ? it1.next() : null;
        }
		
		bw.flush();  // clear LSM writer
		tempFile.close(); // close temp file. 
		
		bfile.close();  // close disk file 
 		
 		// delete disk file and rename temp file to new disk file.
		File fd = new File(filename+".lsm");
		boolean rc = fd.delete();
		if (!rc) throw new RuntimeException("Merge failed. Unable to delete "+filename+".lsm");

		File fm = new File(filename+".temp.lsm");
		rc = fm.renameTo(fd);
		if (!rc) throw new RuntimeException("Merge failed. Unable to rename temp file");

		bfile = new BlockedFile(filename+".lsm", false);  // open the disk file.
		index.close();
		index = new LSMindex(filename+".ism", schema,true);
		buildIndex();
		
		return true;
	}
     
     /*
      * build sparse index over LSMdisk blocks
      */
     void buildIndex() {
// 		 TODO  this will be implemented in week 8.
//    	 scan all tuples in the disk file and use the LSMindex insert(key, blockno) 
//    	 method to create index entries.  After all leaf node entries, call 
//    	 createIndexTree to create non-leaf blocks up to an including root block.  
//    	 Root block must be the last block in index file.
 		 ByteBuffer b  = ByteBuffer.wrap(new  byte[heapdb.Constants.BLOCK_SIZE]);
//    	 LSMindex index = new LSMindex(filename, schema, true);
    	 LSMIterator it = iterator();
    	 Tuple prevBuffer = new Tuple(schema);
    	 int counter = 1;
    	 for(Tuple t: this) {
    		 if(counter==1) {
    			 Tuple currBuffer = t.deserialize(t.getSchema(),b);
    			 index.insertEntry(currBuffer.getKey(), it.blockNo);
    			 prevBuffer = currBuffer;
        		 counter++;
        		 continue;
    		 }
    		 else {
    			 Tuple currBuffer = t.deserialize(t.getSchema(),b);
    			 if(currBuffer != prevBuffer) {
    				 index.insertEntry(currBuffer.getKey(), it.blockNo);
    				 prevBuffer = currBuffer;
    			 }else {
    				 continue;
    			 }

    		 }
    	 }
    	 index.createIndexTree();
    }
	
	@Override 
	public LSMIterator iterator() {
		return new LSMIterator();
	}
	
	public LSMWriter getWriter() {
		return new LSMWriter();
	}
	
	public class LSMWriter {
		private ByteBuffer byte_buffer;
		
		public LSMWriter() {
			byte_buffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
			byte_buffer.position(8);
		}
		
		/**
		 * flush current buffer unless current buffer is empty
		 */
		public void flush() {
			if (byte_buffer.position()> 8) bfile.appendBlock(byte_buffer);
		}
		
		/**
		 * Add a tuple to end of file.
		 * return true 
		 */
		public boolean append(Tuple t) {
			if (t.length() > this.byte_buffer.remaining()) {
				// not enough room for tuple in current buffer
				// write out current buffer and start new buffer.
				bfile.appendBlock(byte_buffer);
				
				// clear the buffer
				byte[] bytes = byte_buffer.array();
				for (int i=0; i<BLOCK_SIZE; i++) 
					bytes[i]=0;
				byte_buffer.position(8);
				
			}
			t.serialize(byte_buffer);
			return true;
		}

	}
	
	public class LSMIterator implements Iterator<Tuple> {
		
		private ByteBuffer byte_buffer;
		private int blockNo = 1;  // block number in buffer.
		private Tuple t;
		
		public LSMIterator() {
			byte_buffer = ByteBuffer.wrap(new byte[BLOCK_SIZE]);
			if (blockNo <= bfile.getHighestBlockNo()) {
				bfile.readBlock(blockNo, byte_buffer);
				t = getNextTuple();
			} else {
				// at end of file. no more data.
				byte_buffer.position(BLOCK_SIZE);
			}
		}
		
		private Tuple getNextTuple() {
			Tuple t = new Tuple(schema);
			for (int icol=0; icol < schema.size(); icol++) {
				switch (schema.getType(icol)) {
					case heapdb.Constants.INT_TYPE:
						t.set(icol, byte_buffer.getInt());
						break;
					case heapdb.Constants.VARCHAR_TYPE:
						int strlen = byte_buffer.getInt();
						byte[] bytes = new byte[strlen];
						byte_buffer.get(bytes);
						t.set(icol,  new String(bytes));
						break;
					default:
						throw new RuntimeException("Unknown column type. "+schema.getType(icol));
				}
			}
			return t;
		}
		
		int blockNo() {
			return blockNo;   // block that contained tuple
		}
	

		@Override
		public boolean hasNext() {
			
			if (t!=null) 
				return true;
			
			if (! byte_buffer.hasRemaining()) {
				// no more data in this buffer.  Read next block.
				blockNo++;
				if (blockNo <= bfile.getHighestBlockNo()) {
					bfile.readBlock(blockNo, byte_buffer);
				} else {
					// at end of file. no more data.
					return false;
					
				}
				
			}
			// get next tuple from current buffer
			t= getNextTuple();
			return true;
		}

		@Override
		public Tuple next() {
			if (t!=null) {
				Tuple r=t;
				t=null;
				return r;
			} else {
				if (hasNext()) return next();
				else return null;
			}
		}

	}
	
	public void printDiagnostic() {
		
		System.out.printf("LSM disk block high %d\n", bfile.getHighestBlockNo());
		// print the LSM disk data file contents
		LSMIterator it = iterator();
		int recno=0;
		int lastBlockNo = -1;
		while (it.hasNext()) {
			int blockNo = it.blockNo();
			if (blockNo!=lastBlockNo) {
				System.out.printf("LSM block %d bytes used %d\n", blockNo, it.byte_buffer.getInt(0));
				lastBlockNo=blockNo;
				recno=0;
			}
			System.out.printf(" Rec %d %s\n", recno, it.next());
			recno++;
		}
        index.printDiagnostic();
	}
}