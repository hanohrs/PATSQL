package patsql.entity.table;

import java.util.Comparator;

public enum Type {
	Int, //
	Dbl, //
	Str, //
	Date, //
	Null;

	public static Type from(String str) {
		return switch (str.toLowerCase()) {
			case "int" -> Int;
			case "dbl" -> Dbl;
			case "str" -> Str;
			case "date" -> Date;
			case "null" -> Null;
			default -> throw new IllegalStateException("invalid type string :" + str);
		};
	}

	public static boolean canBeInserted(Type schemaType, Type cellType) {
		if (schemaType == Null || cellType == Null)
			return true;
		return schemaType == cellType;
	}

	public Comparator<String> comparator() {
		return switch (this) {
			case Int -> Comparator.comparingInt(Integer::parseInt);
			case Dbl -> Comparator.comparingDouble(Double::parseDouble);
			case Str -> String::compareTo;
			case Date -> Comparator.comparing(DateValue::parse);
			case Null -> (s1, s2) -> {
				throw new IllegalStateException("Null cannot have a value.");
			};
		};
	}
}
