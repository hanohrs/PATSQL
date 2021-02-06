package patsql.entity.table.window;

import patsql.entity.table.Cell;
import patsql.entity.table.ColSchema;
import patsql.entity.table.Type;
import patsql.entity.table.agg.Agg;

import java.util.Arrays;

public enum WinFunc {
	ROWNUM, //
	RANK, //
	//
	MAX, //
	MIN, //
	SUM, //
	COUNT, //
	;

	private static boolean uniqueCells(Cell[] orderCells) {
		for (int i = 1; i < orderCells.length; i++)
			if (orderCells[i].equals(orderCells[i - 1]))
				return false;
		return true;
	}

	public boolean hasColParam() {
		return switch (this) {
			case RANK, ROWNUM -> false;
			default -> true;
		};
	}

	public boolean isAgg() {
		return switch (this) {
			case MAX, MIN, SUM, COUNT -> true;
			case ROWNUM, RANK -> false;
		};
	}

	public Type returnType(ColSchema src) {
		return switch (this) {
			case MAX, MIN, SUM -> src == null ? Type.Null : src.type;
			case COUNT, RANK, ROWNUM -> Type.Int;
		};
	}

	public Cell eval(int index, Cell[] targetCells, Cell[] orderCells) {
		if (hasColParam()) {
			if (targetCells.length == 0) {
				return new Cell("null", Type.Null);
			}

			int end = index;
			while (end < orderCells.length - 1 && orderCells[end].equals(orderCells[end + 1])) {
				end++;
			}
			Cell[] tcells = Arrays.copyOfRange(targetCells, 0, end + 1);

			return switch (this) {
				case MAX -> Agg.Max.eval(tcells);
				case MIN -> Agg.Min.eval(tcells);
				case SUM -> Agg.Sum.eval(tcells);
				case COUNT -> Agg.Count.eval(tcells);
				case ROWNUM, RANK -> throw new IllegalStateException("unreachable");
			};
		} else {
			switch (this) {
			case RANK -> {
				int i = index;
				while (i > 0 && orderCells[i].equals(orderCells[i - 1])) {
					i--;
				}
				return new Cell(Integer.toString(i + 1), returnType(null));
			}
			case ROWNUM -> {
				if (!uniqueCells(orderCells))
					return new Cell("null", Type.Null);
				else
					return new Cell(Integer.toString(index + 1), returnType(null));
			}
			default -> throw new IllegalStateException("unreachable");
			}
		}
	}

}
