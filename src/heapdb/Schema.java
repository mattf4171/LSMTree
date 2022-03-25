package heapdb;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;

import static heapdb.Constants.INT_TYPE;
import static heapdb.Constants.VARCHAR_TYPE;
import static heapdb.Constants.MAX_COLUMN_NAME_LENGTH;

public class Schema {

	/*
	 * Lists of column names, data types and max column length. a key column is
	 * optional.
	 */
	private List<String> fnames; // column names
	private List<Integer> ftypes; // column types
	String key; // key column (null if there is no key)

	/*
	 * Create schema with no key
	 */
	public Schema() {
		fnames = new ArrayList<>();
		ftypes = new ArrayList<>();
		key = null;
	}

	/**
	 * add a column
	 * 
	 * @param column  name
	 * @param maximum length
	 */
	public void addVarCharType(String fname) {
		if (fname.length() > MAX_COLUMN_NAME_LENGTH) {
			throw new IllegalArgumentException(
					"field name " + fname + " is more than " + MAX_COLUMN_NAME_LENGTH + " in length");
		}
		fnames.add(fname);
		ftypes.add(VARCHAR_TYPE);
	}

	public void addKeyVarCharType(String fname) {
		if (fname.length() > MAX_COLUMN_NAME_LENGTH) {
			throw new IllegalArgumentException(
					"field name " + fname + " is more than " + MAX_COLUMN_NAME_LENGTH + " in length");
		}
		fnames.add(fname);
		ftypes.add(VARCHAR_TYPE);
		key = fname;

	}

	/**
	 * add a column
	 * 
	 * @param column name
	 */
	public void addIntType(String fname) {
		fnames.add(fname);
		ftypes.add(INT_TYPE);
	}

	public void addKeyIntType(String fname) {
		fnames.add(fname);
		ftypes.add(INT_TYPE);
		key = fname;
	}

	/**
	 * Return the column name of the key. Null if there is no key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Return the index of the column with the given name or -1 if the column name
	 * does not exist in schema.
	 * 
	 * @param column name
	 */
	public int getColumnIndex(String fname) {
		return fnames.indexOf(fname);
	}

	/**
	 * Return the type of the ith field.
	 * 
	 * @param i
	 */
	public int getType(int i) {
		return ftypes.get(i);
	}

	/**
	 * Return the type of the column with the given name, or null if no such name in
	 * this schema.
	 * 
	 * @param fieldName
	 * @return
	 */
	public int getType(String fname) {
		int i = fnames.indexOf(fname);
		return (i < 0) ? null : ftypes.get(i);
	}

	/**
	 * return the name of the ith column
	 * 
	 * @param i
	 */
	public String getName(int i) {
		if (i < 0 || i >= fnames.size()) {
			throw new IllegalArgumentException("No field i in schema: " + this);
		}
		return fnames.get(i);
	}

	/**
	 * Return the number of columns in the schema
	 */
	public int size() {
		return fnames.size();
	}

	/**
	 * return Schema containing a projection of a list of columns.
	 * 
	 * @param attrs is array of columns names to project.
	 */
	public Schema project(String[] attrs) {
		Schema result = new Schema();
		for (int i = 0; i < attrs.length; i++) {
			int k = fnames.indexOf(attrs[i]);
			if (k < 0) {
				throw new IllegalArgumentException("Error: No such column in Schema " + attrs[i]);
			}
			result.fnames.add(fnames.get(k));
			result.ftypes.add(ftypes.get(k));
		}
		return result;
	}

	/**
	 * Return a Schema that is the schema for a natural join of this schema and the
	 * given schema. The returned Schema contains all the columns of this schema
	 * plus those column which are unique to schema s. The returned schema has no
	 * key.
	 * 
	 * @param schema
	 */
	public Schema naturaljoin(Schema s) {
		// copy columns from this schema to new schema
		Schema result = new Schema(); // result schema has no key
		for (int i = 0; i < fnames.size(); i++) {
			result.fnames.add(this.fnames.get(i));
			result.ftypes.add(this.ftypes.get(i));
		}
		// add columns from schema s that are not already in result
		for (int i = 0; i < s.fnames.size(); i++) {
			if (this.fnames.contains(s.fnames.get(i)))
				continue;
			result.fnames.add(s.fnames.get(i));
			result.ftypes.add(s.ftypes.get(i));
		}
		return result;
	}

	/*-
	 * Serialize this to the given byte buffer
	 * 
	 * each column entry is written to buffer as series of bytes as follows 
	 * 4 bytes integer length of column name in bytes 
	 * bytes[] column name 
	 * 4 bytes integer value of column data type 
	 * 4 bytes integer 1=this column is primary key, 0=not a key column
	 */
	public void serialize(ByteBuffer byte_buffer) {
		// serialize schema as tuple (name varchar, type int, isKey int 0=no, 1=yes)
		for (int i = 0; i < fnames.size(); i++) {
			byte_buffer.putInt(fnames.get(i).length());
			byte_buffer.put(fnames.get(i).getBytes());
			byte_buffer.putInt(ftypes.get(i));
			if (key == null || !fnames.get(i).equals(key))
				byte_buffer.putInt(0);
			else
				byte_buffer.putInt(1);
		}

	}

	/**
	 * Create a schema from the given binary data in block 0.
	 * 
	 * @param byte buffer
	 */
	public static Schema deserialize(ByteBuffer byte_buffer) {
		Schema s = new Schema();
		while (byte_buffer.position() < byte_buffer.limit()) {
			int slen = byte_buffer.getInt();
			byte[] bytes = new byte[slen];
			byte_buffer.get(bytes);
			s.fnames.add(new String(bytes));
			s.ftypes.add(byte_buffer.getInt());
			int isKey = byte_buffer.getInt();
			if (isKey == 1) {
				s.key = s.fnames.get(s.fnames.size() - 1);
			}
		}
		return s;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("[");
		for (int i = 0; i < fnames.size(); i++) {
			s.append(fnames.get(i));
			if (ftypes.get(i) == INT_TYPE) {
				s.append(" int");
			} else {
				s.append(" varchar(");
				s.append(")");
			}
			if (i == 0 && key != null)
				s.append(" PRIMARY KEY");
			if (i < fnames.size() - 1)
				s.append(", ");
		}
		s.append("]");
		return s.toString();
	}

}
