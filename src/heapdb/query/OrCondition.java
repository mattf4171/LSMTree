package heapdb.query;

import heapdb.Tuple;

/**
 * A condition of the form cond1 or cond2, where cond1 and cond2 are conditions.
 * 
 * @author Glenn
 *
 */

public class OrCondition extends Condition {

	private Condition cond1, cond2;
	
	public OrCondition(Condition cond1, Condition cond2) {
		this.cond1 = cond1;
		this.cond2 = cond2;
	}
	
	@Override
	public Boolean eval(Tuple tuple) {
		// TODO replace with your code.
		if(cond1.eval(tuple) == null) {
			return null;
		}else if(cond2.eval(tuple) == null) {
			return null;
		}
		if( ( cond1.eval(tuple) || cond2.eval(tuple) ) ) { // check to see if the conditions are true in the table?
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return cond1+" or "+cond2;
	}
}
