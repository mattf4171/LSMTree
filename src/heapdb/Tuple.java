package heapdb;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static heapdb.Constants.INT_TYPE;
import static heapdb.Constants.VARCHAR_TYPE;

public class Tuple {
	protected Object[] values;
	protected Schema schema;    
	
	
	/**
	 * Create a tuple given a schema and column values.
	 * @param schema
	 * @param values
	 */
	
	public Tuple(Schema schema, Object ...values) {
		this.values = values;
		this.schema = schema;
		if (values.length != schema.size()) {
			throw new IllegalArgumentException("Error: Number of values does not match size of schema.");
		}
	}
	
	/**
	 * Create a tuple with 0 for integer columns and null for varchar columns.
	 * @param schema
	 */
	public Tuple(Schema schema) {
		this.schema = schema;
		this.values = new Object[schema.size()];
	}
	
	/**
	 * Copy constructor
	 */
	public Tuple(Tuple rec) {
		this.schema=rec.schema;
		this.values = new Object[schema.size()];
		for (int k=0; k<schema.size(); k++) {
			this.values[k] = rec.values[k];
		}
	}
	
	/**
	 * Compare keys
	 * @return 0 if a equals b, >0 if a is greater than b, <0 if b is greater than a
	 */
	public static int compareKeys(Object a, Object b) {
		if (a instanceof Integer && b instanceof Integer) {
			int i1 = (Integer)a;
			int i2 = (Integer)b;
			return i1-i2;
		} else if (a instanceof String && b instanceof String) {
			String s1=(String)a;
			String s2=(String)b;
			return s1.compareTo(s2);
		} else {
			throw new IllegalArgumentException("Invalid comparision "+a+" , "+b);
		}
	}
	
	public static Tuple joinTuple(Schema s, Tuple t1, Tuple t2) {
		Tuple r = new Tuple(s);
		int t1size = t1.getSchema().size();
		for (int k=0; k<s.size(); k++) {
			if (k<t1size) {
				r.values[k]=t1.values[k];
			} else {
				r.values[k]=t2.get(s.getName(k));
			}
		}
		return r;
	}
	
	/**
	 * get the schema of this tuple
	 */
	public Schema getSchema() {
		return schema;
	}
	
	/**
	 * number of bytes in serialized tuple
	 * each int type is 4 bytes, each string type is 4 + string length
	 */
	public int length() {
		int len=0;
		for (int i=0; i<schema.size(); i++) {
			if (schema.getType(i)==INT_TYPE) {
				len = len + 4;
			} else if (schema.getType(i)==VARCHAR_TYPE) {
				len = len + 4 + ((String)values[i]).length();
			}
		}
		return len;
	}
	
	/**
	 * Return the value of the ith column    
	 * @param column index
	 */
	public Object get(int i) {	
		if (i < 0 || i > schema.size()) {
			throw new IllegalArgumentException("Error: invalid column index. "+i);
		}
		return values[i];
	}
	
	/**
	 * Return the integer value of the ith column    
	 * @param column index
	 */
	public int getInt(int i) {	
		if (i < 0 || i > schema.size()) {
			throw new IllegalArgumentException("Error: invalid column index. "+i);
		}
		if (schema.getType(i)!= INT_TYPE) {
			throw new IllegalArgumentException("Error: invalid column type. "+i);
		}
		int value = (Integer)values[i];
		return value;
	}
	
	/**
	 * Return the String value of the ith column    
	 * @param column index
	 */
	public String getString(int i) {	
		if (i < 0 || i > schema.size()) {
			throw new IllegalArgumentException("Error: invalid column index. "+i);
		}
		if (schema.getType(i)!= VARCHAR_TYPE) {
			throw new IllegalArgumentException("Error: invalid column type. "+i);
		}
		String value = (String)values[i];
		return value;
	}
	
	/**
	 * Return the value of column by column name 
	 * @param column name
	 */
	public Object get(String name) {
		int i = schema.getColumnIndex(name);
		if (i < 0) {
			throw new IllegalArgumentException("Error: invalid column name. "+name);
		}
		return get(i);
	}
	
	/**
	 * Set the value of the ith column 
	 * @param column index
	 * @param value
	 */
	public void set(int i, Object value) {
		if (i < 0 || i >= schema.size()) {
			throw new IllegalArgumentException("Error: column index out of bounds.");
		}
		values[i] = value;
	}
	
	/**
	 * Return the value of the key column.
	 * key is single column value
	 */
	public Object getKey() {
		return get(schema.getKey());
	}
	
	/** 
	 * Return subset of column values.
	 * @param schema containing the columns to project.
	 */
	public Tuple project(Schema schema) {
		Object[] values = new Object[schema.size()];
		for (int i=0; i<schema.size(); i++) {
			values[i] = this.values[this.schema.getColumnIndex(schema.getName(i))];
		}
		Tuple result = new Tuple(schema);
		return result;
	}
	
	public static Tuple deserialize(Schema schema, ByteBuffer byte_buffer) {
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
	
	public void serialize(ByteBuffer byte_buffer) {
		for (int icol=0; icol<schema.size(); icol++) {
			switch (schema.getType(icol)) {
			case heapdb.Constants.INT_TYPE:
				byte_buffer.putInt((int)values[icol]);
				break;
			case heapdb.Constants.VARCHAR_TYPE:
				String s = (String)values[icol];
				byte_buffer.putInt(s.length());
				byte_buffer.put(s.getBytes());
				break;
			default:
				throw new RuntimeException("Unknown column type. "+schema.getType(icol));
			}
		}
	}
	
	public static int keyLength(Object key) {
		if (key instanceof Integer) return 4;
		else if (key instanceof String) return 4 + ((String)key).length();
		else throw new RuntimeException("Key type is invalid. " + key.getClass() + " " + key);
	}
	
	/*
	 * convert tuple key to binary byte[]
	 */
	public static byte[] serializeKey(Object key) {
		byte[] bytes;
		if (key instanceof Integer) {
			bytes = new byte[4];
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			buffer.putInt((int)key);
		} else if (key instanceof String) {
			bytes = new byte[4+((String)key).length()];
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			buffer.putInt(((String)key).length());
			buffer.put(((String)key).getBytes());
		}  else throw new RuntimeException("Key type is invalid. " + key.getClass() + " " + key);
		
		return bytes;
	}
		
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("[");
		for (int i=0; i<values.length; i++) {
			s.append(values[i]);
			if (i < values.length-1) s.append(", ");
		}
		s.append("]");
		return s.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (!Arrays.deepEquals(values, other.values))
			return false;
		return true;
	}
}
