package heapdb.query;

import heapdb.Tuple;

/**
 * A condition of the form colName = value.
 * 
 * @author Glenn
 *
 */

public class EqCondition extends Condition {

	private String colName;
	private Object value;

	public EqCondition(String colName, Object value) {
		this.colName = colName;
		this.value = value;
	}

	public String getColumnName() {
		return this.colName;

	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public Boolean eval(Tuple tuple) {
		if(tuple.get(colName).equals(value)){ return true; }
		return false;
	}

	@Override
	public String toString() {
		return colName + " = " + value;
	}
}
