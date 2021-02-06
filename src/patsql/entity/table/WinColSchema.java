package patsql.entity.table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import patsql.entity.table.agg.GroupKeys;
import patsql.entity.table.sort.SortKey;
import patsql.entity.table.sort.SortKeys;
import patsql.entity.table.window.WinFunc;

import java.util.Optional;

public class WinColSchema extends ColSchema {
	public final WinFunc func;
	public final GroupKeys partKey;
	public final SortKeys orderKey;
	private final ColSchema src;

	/**
	 * @param func The window function
	 * @param src  The column to be aggregated. {@code null} if no such column required.
	 * @param pKey The column used in PARTITION BY in SQL.
	 *             Specify {@link GroupKeys#nil()} if PARTITION BY not required.
	 * @param oKey The column used in ORDER BY. Only one column can be specified for now.
	 */
	public WinColSchema(WinFunc func, @Nullable ColSchema src, @NotNull GroupKeys pKey, @Nullable SortKey oKey) {
		super(str(func, src, pKey, oKey), func.returnType(src));
		this.func = func;
		this.src = src;
		this.partKey = pKey;
		this.orderKey = (oKey == null) ? SortKeys.nil() : new SortKeys(oKey);
	}

	public Optional<ColSchema> getSrc() {
		return Optional.ofNullable(src);
	}

	private static String str(WinFunc func, ColSchema src, GroupKeys pKey, SortKey oKey) {
		return func + "(" + ((src == null) ? "" : src.name) + ")("//
				+ ((pKey == null) ? "" : pKey)//
				+ ","//
				+ ((oKey == null) ? "" : oKey)//
				+ ")";
	}

	public boolean hasOrderKey() {
		return orderKey.keys.length != 0;
	}

	public boolean isValid() {
		switch (func) {
		case COUNT:
		case SUM:
		case MAX:
		case MIN:
			if (src == null)
				return false;
			if (!hasOrderKey()) // excluded because it is the same as GroupBy+Join
				return false;
			break;
		case RANK:
			if (!hasOrderKey())
				return false;
			break;
		case ROWNUM:
			// TODO: the order key must assure the uniqueness, but current implementation
			//       does not when the key has duplicated values.
			if (!hasOrderKey())
				return false;
			//noinspection VariableNotUsedInsideIf
			if (src != null)
				return false;
			break;
		}
		return true;
	}

}
