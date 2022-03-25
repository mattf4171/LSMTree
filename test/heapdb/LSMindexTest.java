package heapdb;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class LSMindexTest {
	
	Random gen = new Random();
	

	@Test
	//@Disabled
	public void intKeyTest() {
		
		final int LOW_KEY = -500;
		final int HI_KEY = 500;
		
		Schema s = new Schema();
		s.addKeyIntType("ID");
		
		LSMindex index = new LSMindex("intTest.ism", s, true );
		
		try { 
			for (int i = LOW_KEY; i <= HI_KEY; i++) {
				index.insertEntry( i, 10000+i);
			}

			index.createIndexTree();
			index.printDiagnostic();
			index.close();
			
			index = new LSMindex("intTest.ism", s, false);

			for (int i=0; i<1000; i++) {
				int key = (-900) + gen.nextInt(1800);  // random int 
				int blockno = index.lookup(key);
				if (key<LOW_KEY ) assertEquals("key="+key, Integer.MIN_VALUE, blockno);
				else if (key > HI_KEY) assertEquals("key="+key, 10500, blockno);
				else assertEquals("key="+key, 10000+key, blockno);
			}
			index.close();
		} catch (Error e) {
			index.close();
			throw e;
		}
	}
	
	@Test
	//@Disabled
	public void stringKeyTest() {
		
		final int LOW_KEY = 1000;
		final int HI_KEY = 2000;
		
		Schema s = new Schema();
		s.addKeyVarCharType("name");
		LSMindex index = new LSMindex("stringTest.ism", s, true);
		try {
			for (int i = LOW_KEY; i <= HI_KEY; i++) {
				String skey = String.format("test%04d",i).repeat((i%3) + 1);
				index.insertEntry(skey,  i+1);
			}
			index.createIndexTree();
			index.printDiagnostic();
			index.close();
			

			index = new LSMindex("stringTest.ism", s, false);
			for (int i=0; i<1000; i++) {
				int key = gen.nextInt(HI_KEY+100);  
				String skey = String.format("test%04d",key).repeat((key%3) + 1);
				int blockno = index.lookup(skey);
				if (key<LOW_KEY ) assertEquals("key="+key, Integer.MIN_VALUE, blockno);
				else if (key > HI_KEY) assertEquals("key="+key, Math.abs(HI_KEY+1), blockno);
				else assertEquals("key="+key, Math.abs(key+1), blockno);
			}
			index.close();
		} catch (Error e) {
			index.close();
			throw e;
		}
	}

}
