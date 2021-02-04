package patsql.entity.table;

import java.text.DecimalFormat;
import java.util.Comparator;

public class Cell implements Comparable<Cell> {
	public final Type type;
	public final String value;

	public Cell(String value, Type type) {
		// validation and formatting
		switch (type) {
		case Int:
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(value);
			break;
		case Dbl:
			double d = Double.parseDouble(value);
			DecimalFormat fm = new DecimalFormat("#.000");
			value = fm.format(d);
			break;
		case Null:
			value = "NULL";
			break;
		case Str:
			// NOP
			break;
		case Date:
			DateValue dateCell = DateValue.parse(value);
			value = dateCell.toString();
			break;
		}

		this.type = type;
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int compareTo(Cell other) {
		assert this.type == other.type || this.type == Type.Null || other.type == Type.Null;

		Comparator<String> comparator;
		switch (type) {
		case Int:
			comparator = Comparator.comparingInt(Integer::parseInt);
			break;
		case Dbl:
			comparator = Comparator.comparingDouble(Double::parseDouble);
			break;
		case Str:
			comparator = String::compareTo;
			break;
		case Date:
			comparator = Comparator.comparing(DateValue::parse);
			break;
		case Null:
			comparator = (s1, s2) -> {
				throw new IllegalStateException("Null cannot have a value.");
			};
			break;
		default:
			throw new IllegalStateException("unknown type.");
		}

		return Comparator.comparing(
				(Cell cell) -> cell.type == Type.Null ? null : cell.value,
				Comparator.nullsFirst(comparator)
		).compare(this, other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Cell)) return false;
		Cell other = (Cell) obj;
		if (type != other.type)
			return false;
		return compareTo(other) == 0;
	}

}
