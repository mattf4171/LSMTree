package heapdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Table implements ITable {
	
	/*
	 * 
	 * @author Matthew Fernandez 
	 */
	
	private List<Tuple> tuples;
	private Schema schema;
	
	public Table(Schema schema) {
		this.schema = schema;
		tuples = new ArrayList<>();
	}
	
	@Override
	public Schema getSchema() {
		return schema;
	}

	
	@Override
	public int size() {
		return tuples.size();
	}

	@Override
	public void close() {
		// do nothing
	}
	
	@Override
	public boolean insert(Tuple rec) {
		if (! rec.getSchema().equals(schema)) {
			throw new IllegalStateException("Error: tuple schema does not match table schema.");
		}
		if(schema.getKey()!=null) { // to pass the query projectStrQuery() test
			for (Tuple t: tuples) {
				if(t.getKey().equals(rec.getKey())) { // condition to check for duplicates
					return false;
				}
			}
		}
		tuples.add(rec); // no duplicate, than add to tuple array
	    return true;
	
		
	}

	@Override
	public boolean delete(Object key) {
		if (schema.getKey() == null) {
			throw new IllegalStateException("Error: table does not have a primary key.  Can not delete.");
		}
		
		for(Tuple t: tuples) {
			if(  key.equals(t.getKey()) ) { 
				tuples.remove(t); // method to remove the desired element in array tuples
				return true;
			}
		}
		return false;
	}


	@Override
	public Tuple lookup(Object key) { // look up key in tuples,
		if (schema.getKey() == null) {
			throw new IllegalStateException("Error: table does not have a primary key.  Can not lookup by key.");
		}

//		Schema s = new Schema();
		for(Tuple t: tuples) {
			if(t.getKey().equals(key)) {
				return t;
			}else if(t==tuples.get(tuples.size()-1)){
				return null;
			}
		}
		return null;
	}

	@Override
	public ITable lookup(String colname, Object value) { // if colname and value are present in table, insert it into the ITable Obj
		if (schema.getColumnIndex(colname) < 0) {
			throw new IllegalStateException("Error: table does not contain column "+colname);
		}
		ITable t = new Table(schema);
		for(Tuple i: tuples) {
			if(i.get(colname).equals(value)) {
				t.insert(i);
//				System.out.print(" Successful, found in table.\n");
			}else if(i==tuples.get(tuples.size()-1)){ // no in table condition
//				System.out.print(" Failed, not found in table.\n");
			}
		}
		return t;
	}

	@Override
	public Iterator<Tuple> iterator() {
		return new TIterator(tuples.iterator());
	}
	
	@Override
	public String toString() {
		
		// TODO implement this method
		if (schema.size() ==0) {
			return "Empty Table";
		}else {
			StringBuilder sb = new StringBuilder();
			for (Tuple t : this) {
				sb.append(t.toString());
				sb.append("\n");
			}
			return sb.toString();
		}
	}
	
	/*
	 * An iterator that returns a copy of each tuple in 
	 * the table.
	 */
	public static class TIterator implements Iterator<Tuple> {
		
		private Iterator<Tuple> it;
		
		public TIterator(Iterator<Tuple> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Tuple next() {
			return new Tuple(it.next());
		}	
	}
}