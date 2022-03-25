package heapdb.query;


import java.util.ArrayList;

import heapdb.ITable;
import heapdb.Schema;
import heapdb.Table;
import heapdb.Tuple;

/**
 * A simple select query of the form:
 * select column, column, . . . from table where condition
 * 
 * @author Matthew Fernandez
 *
 */

public class SelectQuery  {
	
	private Condition cond;
	private String[] colNames;	   // a value of null means return all columns of the table
	
	/**
	 * A query that contains both a where condition and a projection of columns
	 * @param colNames are the columns to return
	 * @param cond is the where clause
	 */
	public SelectQuery(String[] colNames, Condition cond) {
		this.colNames = colNames;
		this.cond = cond;
	}
	
	/**
	 * A query that contains both a where condition.  All columns 
	 * of the Tuples are returned.
	 * @param cond is the where clause
	 */
	public SelectQuery(Condition cond) {
		this(null, cond);
	}
	
	
	public static Table naturalJoin(ITable table1, ITable table2) {

		Schema resSchema = table1.getSchema().naturaljoin(table2.getSchema()); 
		Table tbl = new Table(resSchema);
		
		// get the column names that are duplicated and put that in a list
		ArrayList<String> list = new ArrayList<>();
		for (int i =0; i<table1.getSchema().size(); i++) {
			for(int j = 0; j< table2.getSchema().size(); j++) {
				// check to see if the columnname in table 1 is in table 2
				if( table1.getSchema().getName(i).equals(table2.getSchema().getName(j)) ) {
					// if it exists in the list don't add the value again
					if(! list.contains(table1.getSchema().getName(i))){
						list.add(table1.getSchema().getName(i)); // list of duplicated columns
					}
				}
			}
		}
		
		//nested loop join using values of duplicated list
		for(Tuple t1: table1) {
			for(Tuple t2: table2) {
				Boolean match = true;
				// loop through shared duplicate 
				for(int i=0; i < list.size(); i++) {
					// check to see that the row at the column name is duplicated in table 2
					if(! (t1.get(list.get(i))).equals(t2.get(list.get(i))) ){
						match = false;
						break;
					}
				}
				if(match==true) {
					// continue on with naturalJoin implementation
					Tuple r = Tuple.joinTuple(resSchema, t1, t2); // this is the joined tuple
					tbl.insert(r);
				}
			}
		}
		return tbl;
		
	}
	
	public ITable eval(ITable table) {		
//		1. create a result Schema if colName is not null. Use Schema.project method (line 130).
//		2. create a result table
//		3. Iterate over all tuple in table given as argument
//		4. for each row, cond.eval(tuple) 
//		5. if false skip to next row.
//		6. if true -> project from tuple, using Tuple.project method (line 180)
//		7. return result table.
		Schema s;
		if(this.colNames != null) { // project from tuple, using Tuple.project
			s = table.getSchema().project(colNames);
			ITable tbl = new Table(s); // result table
			
			for(Tuple t: table) { // create result table
				if(cond.eval(t)) {
					tbl.insert(t.project(s));
				}
			}
			return tbl;

		}else {
			s = table.getSchema(); // colNames is null
		
			ITable tbl = new Table(s); // result table
			
			for(Tuple t: table) { // create result table
				if(cond.eval(t)) {
					tbl.insert(t);
				}
			}
			return tbl;
		}
	}

	@Override
	public String toString() {
	    String proj_columns;
	    if (colNames != null) {
	    	proj_columns = String.join(",", colNames);
	    } else {
	    	proj_columns = "*";
	    }
	    return "select " + proj_columns + " where " + cond;
	}

}
