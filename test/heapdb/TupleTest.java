package heapdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TupleTest {
	
	static Schema schema;
	// return a new record with the given field values
	static Tuple createTestRecord() {
		// create a schema with three integer fields, named 'a', 'b', 'c',
		// with 'a' the key
		schema = new Schema();
		schema.addIntType("a");
		schema.addIntType("b");
		schema.addIntType("c");
		
		Tuple rec = new Tuple(schema, 1, 1, 1);
		return rec;
	}

	// construct a simple record and check value of the field
	@Test
	void createSimpleRecord() {
		Schema schema = new Schema();
		schema.addIntType("a");
		Tuple rec = new Tuple(schema, 1);
		assertEquals(rec.get(0), 1);
	}
	
	// the Tuple constructor should throw an exception if schema doesn't
	// match supplied field values
	@Test
	void recordsMatchSchema() {
		Schema schema = new Schema();
		schema.addIntType("a"); 
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> 
			new Tuple(schema, 1, 2));
	}
}