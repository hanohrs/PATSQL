package patsql.entity.table;

public enum Type {
	Int, //
	Dbl, //
	Str, //
	Date, //
	Null;

	public static Type from(String str) {
		switch (str.toLowerCase()) {
		case "int":
			return Int;
		case "dbl":
			return Dbl;
		case "str":
			return Str;
		case "date":
			return Date;
		case "null":
			return Null;
		default:
			throw new IllegalStateException("invalid type string :" + str);
		}
	}

	public static boolean canBeInserted(Type schemaType, Type cellType) {
		if (schemaType == Null || cellType == Null)
			return true;
		return schemaType == cellType;
	}

}
