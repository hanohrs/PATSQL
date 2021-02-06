package patsql.entity.table.sort;

public record SortKeys(SortKey... keys) {
	public static final SortKeys NIL = new SortKeys();

	public static SortKeys nil() {
		return NIL;
	}

	public SortKey[] getAll() {
		return keys;
	}

	public SortKey get(int i) {
		return keys[i];
	}

	public int count() {
		return keys.length;
	}

	@Override
	public String toString() {
		if (keys.length == 0) {
			return "nil";
		}

		String[] strs = new String[keys.length];
		for (int i = 0; i < strs.length; i++) {
			strs[i] = keys[i].toString();
		}
		return String.join(" ", strs);
	}
}
