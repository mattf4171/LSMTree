package heapdb;


import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableTest {
	
	private Table table;
	private Tuple oldTup, newTup;
	
	@BeforeEach
	void init() {
		Schema schema = new Schema();  
		schema.addKeyIntType("ID");   // primary key ID int 
		schema.addVarCharType("name");
		schema.addVarCharType("dept_name");
		schema.addIntType("salary");
		Tuple[] tuples = new Tuple[] {
				new Tuple(schema, 22222, "Einstein",    "Physics", 95000),
				new Tuple(schema, 12121, "Wu",          "Finance", 90000),
				new Tuple(schema, 32343, "El Said" ,    "History", 60000),
				new Tuple(schema, 45565, "Katz",        "Comp. Sci.", 75000),
				new Tuple(schema, 98345, "Kim",         "Elec. Eng.", 80000),
				new Tuple(schema, 10101, "Srinivasan" , "Comp. Sci.", 65000),
				new Tuple(schema, 76766, "Crick" ,      "Biology", 72000),
		};
		table = new Table(schema);
		for (int i = 0; i < tuples.length; i++) {
			table.insert(tuples[i]);
		}
		
		oldTup = new Tuple(schema, 22222, "Einstein",    "Physics", 95000 );
		newTup = new Tuple(schema, 11111, "Molina",      "Music",   70000 );
	}

	@Test
	void insertOneTuple() {
		// insert should succeed if the key value is not already in the table
		boolean insertSucceeded = table.insert(newTup);
		assertTrue(insertSucceeded);
	}
	
	@Test
	void insertDuplicateTuple() {
		// insert should fail if the key value *is* already in the table
		boolean insertSucceeded = table.insert(oldTup);
		assertTrue(!insertSucceeded);
	}
	
	@Test
	void lookupExistingTuple() {
		// lookup by ID should succeed if the ID is in the table
		int ID = oldTup.getInt(0);
		ITable tuples = table.lookup("ID", ID);
		assertTrue(tuples.size()==1);
		Tuple tuple = tuples.iterator().next();
		assertTrue(tuple.getInt(0)==ID);
	}
	
	@Test
	void lookupMissingTuple() {
		// lookup by ID should fail if the ID is not in the table
		ITable tuples = table.lookup("ID", newTup.getInt(0));
		assertTrue(tuples.size()==0);
	}
}
	