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
	private WinColSchema(WinFunc func, @Nullable ColSchema src, @NotNull GroupKeys pKey, @Nullable SortKey oKey) {
		super(str(func, src, pKey, oKey), func.returnType(src));
		this.func = func;
		this.src = src;
		this.partKey = pKey;
		this.orderKey = (oKey == null) ? SortKeys.nil() : new SortKeys(oKey);
	}

	public static Optional<WinColSchema> newInstance(WinFunc func, @Nullable ColSchema src, @NotNull GroupKeys pKey, @Nullable SortKey oKey) {
		if (isValid(func, src, pKey, oKey))
			return Optional.of(new WinColSchema(func, src, pKey, oKey));
		else
			return Optional.empty();
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
		return orderKey.count() != 0;
	}

	private static boolean isValid(WinFunc func, @Nullable ColSchema src, @NotNull GroupKeys pKey, @Nullable SortKey oKey) {
		return switch (func) {
			// oKey == null is excluded because it is the same as GroupBy+Join
			case COUNT, MAX, MIN -> src != null && oKey != null;
			case SUM -> src != null && oKey != null && switch (src.type) {
				case Int, Dbl -> true;
				case Null, Str, Date -> false;
			};
			case RANK -> oKey != null;
			// TODO: the order key must assure the uniqueness, but current implementation
			//       does not when the key has duplicated values.
			case ROWNUM -> oKey != null && src == null;
		};
	}

}
