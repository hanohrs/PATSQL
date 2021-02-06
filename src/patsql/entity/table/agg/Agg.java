package patsql.entity.table.agg;

import patsql.entity.table.Cell;
import patsql.entity.table.Type;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Agg {
	Max,
	Min,
	Count,
	CountD,
	Avg,
	Sum,
	ConcatComma, // separated by comma
	ConcatSpace, // separated by space
	ConcatSlash, // separated by slash
	;

	public Type returnType(Type operand) {
		return switch (this) {
			case Max, Min, Sum -> operand;
			case Count, CountD -> Type.Int;
			case Avg -> Type.Dbl;
			case ConcatComma, ConcatSpace, ConcatSlash -> Type.Str;
		};
	}

	public boolean isComputable(Type operand) {
		if (operand == Type.Null)
			return false;

		return switch (this) {
			case Max, Min, Count, CountD -> true;
			case Sum, Avg -> operand == Type.Int || operand == Type.Dbl;
			case ConcatComma, ConcatSpace, ConcatSlash -> operand == Type.Str;
		};
	}

	public Cell eval(Cell[] cells) {
		final Type operandType;
		final Type retType;

		if (cells.length == 0)
			return Cell.NULL;

		Optional<Cell> firstNonNull = Arrays.stream(cells).filter(c -> c.type() != Type.Null).findAny();
		if (firstNonNull.isEmpty())
			return Cell.NULL;
		else {
			operandType = firstNonNull.get().type();
			retType = returnType(operandType);
		}

		Stream<String> nonNullCellValues = Arrays.stream(cells)
				.filter(c -> c.type() != Type.Null)
				.map(Cell::value);

		String retValue = switch (this) {
			case Max -> nonNullCellValues.max(operandType.comparator()).orElseThrow();
			case Min -> nonNullCellValues.min(operandType.comparator()).orElseThrow();
			case Sum -> switch (operandType) {
				case Int -> Integer.toString(nonNullCellValues.mapToInt(Integer::parseInt).sum());
				case Dbl -> Double.toString(nonNullCellValues.mapToDouble(Double::parseDouble).sum());
				case Str, Date, Null -> throw new IllegalStateException("Sum over " + operandType);
			};
			case Count -> Long.toString(nonNullCellValues.count());
			case CountD -> Integer.toString(nonNullCellValues.collect(Collectors.toSet()).size());
			case Avg -> Double.toString(switch (operandType) {
				case Int -> nonNullCellValues.mapToInt(Integer::parseInt).average().orElseThrow();
				case Dbl -> nonNullCellValues.mapToDouble(Double::parseDouble).average().orElseThrow();
				case Str, Date, Null -> throw new IllegalStateException("Avg over " + operandType);
			});
			case ConcatComma, ConcatSpace, ConcatSlash -> {
				String delimiter = switch (this) {
					case ConcatComma -> ", ";
					case ConcatSpace -> " ";
					case ConcatSlash -> "/";
					case Max, Min, Count, CountD, Avg, Sum -> throw new IllegalStateException(String.format("%s over %s", this, operandType));
				};
				yield nonNullCellValues.collect(Collectors.joining(delimiter));
			}
		};
		return new Cell(retValue, retType);
	}
}
