package heapdb;

/*
 * Marker class for a deleted tuple.
 * Contains only the key value of the deleted tuple.
 */
class TupleDeleted extends Tuple { // Tombstone

	TupleDeleted(Schema schema, Object key) {
		super(schema);
		int colIndex = schema.getColumnIndex(schema.getKey());
		this.set(colIndex, key);
	}
	
	/*
	 * Copy constructor to create a deleted tuple from a tuple.
	 */
	TupleDeleted(Tuple t) {
		super(t.getSchema());
		int colIndex = schema.getColumnIndex(schema.getKey());
		this.set(colIndex, t.get(colIndex));
	}

}
