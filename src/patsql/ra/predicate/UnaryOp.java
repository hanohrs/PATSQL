package patsql.ra.predicate;

import patsql.entity.table.Cell;
import patsql.entity.table.Type;

public enum UnaryOp {
	IsNull("IS NULL"),
	IsNotNull("IS NOT NULL");

	private final String str;

	UnaryOp(String str) {
		this.str = str;
	}

	@Override
	public String toString() {
		return str;
	}

	public ExBool eval(Cell cell) {
		return switch (this) {
			case IsNull -> cell.type() == Type.Null ? ExBool.True : ExBool.False;
			case IsNotNull -> cell.type() != Type.Null ? ExBool.True : ExBool.False;
		};
	}
}
