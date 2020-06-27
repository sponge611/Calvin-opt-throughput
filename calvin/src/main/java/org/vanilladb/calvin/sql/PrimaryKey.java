package org.vanilladb.calvin.sql;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.sql.Type;
import org.vanilladb.core.sql.predicate.ConstantExpression;
import org.vanilladb.core.sql.predicate.Expression;
import org.vanilladb.core.sql.predicate.FieldNameExpression;
import org.vanilladb.core.sql.predicate.Predicate;
import org.vanilladb.core.sql.predicate.Term;
import org.vanilladb.core.storage.index.SearchKey;

public class PrimaryKey implements Serializable {

	private static final long serialVersionUID = 20200107001L;
	
	private String tableName;
	private String[] fields;
	// We serialize this field manually
	// because a Constant is non-serializable.
	private transient Constant[] values;
	private int hashCode;
	
	
	public PrimaryKey(String tableName, String fld, Constant val) {
		this.tableName = tableName;
		this.fields = new String[1];
		this.values = new Constant[1];
		fields[0] = fld;
		values[0] = val;
		this.calculateHashCode();
	}
	
	/**
	 * Constructs a PrimaryKey with the given field array and value array.
	 * This method should be only called by {@link PrimaryKeyBuilder}.
	 * 
	 * @param tableName
	 * @param fields
	 * @param values
	 */
	PrimaryKey(String tableName, String[] fields, Constant[] values) {
		if (fields.length != values.length)
			throw new IllegalArgumentException();
		
		this.tableName = tableName;
		this.fields = fields;
		this.values = values;
		this.calculateHashCode();
	}

	public String getTableName() {
		return tableName;
	}
	
	public boolean containsField(String fld) {
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equals(fld))
				return true;
		}
		return false;
	}
	
	public int getNumOfFlds() {
		return fields.length;
	}
	
	public String getField(int index) {
		return fields[index];
	}
	
	public Constant getVal(int index) {
		return values[index];
	}

	public Constant getVal(String fld) {
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equals(fld))
				return values[i];
		}
		return null;
	}

	public Predicate toPredicate() {
		Predicate pred = new Predicate();
		for (int i = 0; i < fields.length; i++) {
			Expression k = new FieldNameExpression(fields[i]);
			Expression v = new ConstantExpression(values[i]);
			pred.conjunctWith(new Term(k, Term.OP_EQ, v));
		}
		return pred;
	}
	
	public SearchKey toSearchKey(List<String> indexedFields) {
		Constant[] vals = new Constant[indexedFields.size()];
		Iterator<String> fldNameIter = indexedFields.iterator();

		for (int i = 0; i < vals.length; i++) {
			String fldName = fldNameIter.next();
			vals[i] = getVal(fldName);
			if (vals[i] == null)
				throw new NullPointerException("there is no value for '" + fldName + "'");
		}
		
		return new SearchKey(vals);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(tableName);
		sb.append(": ");
		for (int i = 0; i < fields.length; i++) {
			sb.append(fields[i]);
			sb.append(" -> ");
			sb.append(values[i]);
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append("}");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj.getClass() != PrimaryKey.class)
			return false;
		PrimaryKey k = (PrimaryKey) obj;
		return k.tableName.equals(this.tableName) && Arrays.equals(k.fields, this.fields)
				&& Arrays.equals(k.values, this.values);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
	
	private void calculateHashCode() {
		this.hashCode = 17;
		this.hashCode = 31 * this.hashCode + tableName.hashCode();
		for (int i = 0; i < fields.length; i++) {
			this.hashCode = 31 * this.hashCode + this.fields[i].hashCode();
			this.hashCode = 31 * this.hashCode + this.values[i].hashCode();
		}
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeInt(values.length);

		// Write out all elements in the proper order
		for (int i = 0; i < values.length; i++) {
			Constant val = values[i];
			byte[] bytes = val.asBytes();
			out.writeInt(val.getType().getSqlType());
			out.writeInt(val.getType().getArgument());
			out.writeInt(bytes.length);
			out.write(bytes);
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		int numberOfVals = in.readInt();
		this.values = new Constant[numberOfVals];

		// Read in all values
		for (int i = 0; i < numberOfVals; i++) {
			int sqlType = in.readInt();
			int argument = in.readInt();
			byte[] bytes = new byte[in.readInt()];
			in.read(bytes);
			Constant val = Constant.newInstance(Type.newInstance(sqlType, argument), bytes);
			values[i] = val;
		}
	}
}
