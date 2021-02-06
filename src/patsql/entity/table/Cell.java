package patsql.entity.table;

import java.text.DecimalFormat;
import java.util.Comparator;

public record Cell(String value, Type type) implements Comparable<Cell> {
	private static final String NULL_VALUE = "NULL";
	public static final Cell NULL = new Cell(NULL_VALUE, Type.Null);

	public Cell {
		switch (type) {
		case Int -> {//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(value); // validation
		}
		case Dbl -> {
			double d = Double.parseDouble(value);
			DecimalFormat fm = new DecimalFormat("#.000");
			value = fm.format(d);
		}
		case Null -> {
			value = NULL_VALUE;
		}
		case Str -> {
			// NOP
		}
		case Date -> {
			DateValue dateCell = DateValue.parse(value);
			value = dateCell.toString();
		}
		}
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int compareTo(Cell other) {
		assert this.type == other.type || this.type == Type.Null || other.type == Type.Null;
		//noinspection ReturnOfNull
		return Comparator.comparing(
				(Cell cell) -> cell.type == Type.Null ? null : cell.value,
				Comparator.nullsFirst(this.type.comparator())
		).compare(this, other);
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		if (type != Type.Null)
			result = 31 * result + value.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Cell other) || type != other.type)
			return false;
		return compareTo(other) == 0;
	}
}
