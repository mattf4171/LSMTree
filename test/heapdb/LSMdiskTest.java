package heapdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LSMdiskTest {
	
	static LSMdisk disk;
	
	@BeforeEach
	public void init() {
		Schema sch = new Schema();
		sch.addKeyIntType("ID");
		sch.addVarCharType("name");
		disk = new LSMdisk("disktest", sch);
		LSMdisk.LSMWriter wr = disk.getWriter();
		Tuple t = new Tuple(sch);
		for (int i=10000; i<10006; i++) {
			t.set(0,i);
			t.set(1,"name"+i);
			wr.append(t);
		}
		wr.flush();
		disk.buildIndex();
		disk.close();
		disk = new LSMdisk("disktest");
		disk.printDiagnostic();
	}
	
	@AfterEach
	public void cleanup() {
		if (disk!=null) disk.close();
	}

	// Test lookup found
	@Test
	public void lookup() {
		for (int i=10000; i<10006; i++) {
			Tuple t = disk.lookup(i);
			if (t==null) System.out.println(i);
			assertNotNull(t);
			assertEquals(i, t.getKey());
			assertEquals(i, t.getInt(0));
			assertEquals("name"+i, t.getString(1));
		}
	}
	
	// Test lookup not found
	@Test
	public void lookupNotFound() {
		Tuple t = disk.lookup(9999);
		assertNull(t);
		t = disk.lookup(0);
		assertNull(t);
		t = disk.lookup(99999);
		assertNull(t);
	}

	@Test
	public void diagnostic() {
		disk.printDiagnostic();
	}

}
