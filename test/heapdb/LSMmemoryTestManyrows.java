package heapdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class LSMmemoryTestManyrows {
	
	@Test
	public void rows1000() {
		Schema s = new Schema();
		s.addKeyIntType("ID");
		s.addVarCharType("name" );
		s.addVarCharType("dept_name" );
		s.addIntType("salary");
		
		
		String[] depts = {"Biology", "Chemistry", "Physics", "Comp. Sci.", "Mathematics", "Statistics", "Pschology", "Spanish", "Latin", "Greek", 
				"English", "Japanese", "Chinese", "Communication", "Cinemtogrphy", "Art"};
		Tuple t = new Tuple(s);
		Random gen = new Random();
		int inserts = 0;
		int deletes = 0;
		
		LSMmemory table = new LSMmemory("rows1000", s);
		int[] keys = new int[1001];
		
		
		for (int i=0; i<10000; i++) {
			int rkey = gen.nextInt(1000)+1;
			if (keys[rkey]==0) {
				t.set(0, rkey);
				t.set(1, "Name"+rkey);
				t.set(2, depts[gen.nextInt(depts.length)]);
				t.set(3, gen.nextInt(50000)+50000);
				boolean result = table.insert(t);
				assertTrue(result);
				keys[rkey]=rkey;
				inserts++;
			} else {
				Tuple t1 = table.lookup(rkey);
				assertNotNull(t1);
				assertEquals(rkey, t1.getInt(0));
				boolean result = table.delete(rkey);
				assertTrue(result);
				keys[rkey]=0;
				deletes++;
			}
		}
		System.out.printf("inserts = %d, deletes=%d\n", inserts, deletes);
	
		for (int i=0; i<keys.length; i++) {
			if (keys[i]!=0) {
				Tuple t2 = table.lookup(keys[i]);
				assertNotNull(t2);
				assertEquals(keys[i], t2.getInt(0));
			}
		}
	}

}
