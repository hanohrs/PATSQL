package patsql.synth.filler.strategy;

import patsql.entity.synth.Example;
import patsql.entity.synth.SynthOption;
import patsql.entity.table.AggColSchema;
import patsql.entity.table.BitTable;
import patsql.entity.table.Cell;
import patsql.entity.table.ColSchema;
import patsql.entity.table.Table;
import patsql.entity.table.Type;
import patsql.ra.operator.RA;
import patsql.ra.operator.RAOperator;
import patsql.ra.operator.Selection;
import patsql.ra.predicate.BinaryOp;
import patsql.ra.predicate.BinaryPred;
import patsql.ra.predicate.Conjunction;
import patsql.ra.predicate.UnaryOp;
import patsql.ra.predicate.UnaryPred;
import patsql.ra.util.RAUtils;
import patsql.synth.filler.BitRow;
import patsql.synth.filler.ColConstraint;
import patsql.synth.filler.ColRelation;
import patsql.synth.filler.FillingConstraint;
import patsql.synth.filler.RowSearchConsumingAllConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectionAllConstantsUsed implements FillingStrategy {

	final FillingConstraint constraint;

	/**
	 * a weak constraint used in the process of filling.
	 */
	final FillingConstraint preConstraint;

	public SelectionAllConstantsUsed(FillingConstraint constraint) {
		super();
		this.constraint = constraint;

		ColRelation colRel = constraint.col.relation.update(RA.SELECTION);
		this.preConstraint = new FillingConstraint(
				new ColConstraint(constraint.col.matching, colRel)
		);
	}

	@Override
	public RA targetKind() {
		return RA.SELECTION;
	}

	/**
	 * Order comparisons between String types are excluded.
	 */
	private static boolean isTried(ColSchema left, BinaryOp binop, Cell right) {
		if (left.type != right.type())
			return false;

		// skip 'x' = ConcatConmma(...)
		if (left instanceof AggColSchema ag) {
			switch (ag.agg) {
			case ConcatComma, ConcatSlash, ConcatSpace:
				return false;
			}
		}

		if (left.type == Type.Str) {
			return switch (binop) {
				case Eq, NotEq -> true;
				case Gt, Geq, Lt, Leq -> false;
			};
		}
		return true;
	}

	@Override
	public List<RAOperator> fill(RAOperator sketch, Example example, SynthOption option) {
		if (sketch.kind != targetKind()) {
			throw new IllegalStateException("Invalid filling strategy.");
		}
		// option = remainingOption(option, sketch); // Disabled now.

		Selection target = (Selection) sketch;
		Table outTable = example.output;
		BitTable tmpTable = new BitTable(target.child.eval(example.tableEnv()));

		if (preConstraint.isPruned(tmpTable, outTable)) {
			return Collections.emptyList();
		}

		Set<Cell> alreadyUsed = RAUtils.usedConstants(target.child);
		List<Cell> mustBeUsed = Arrays.stream(option.extCells)
				.filter(c -> !alreadyUsed.contains(c))
				.collect(Collectors.toCollection(ArrayList::new));

		RowSearchConsumingAllConstants search = new RowSearchConsumingAllConstants(
				tmpTable.height(),
				mustBeUsed.toArray(Cell[]::new)
		);

		for (ColSchema col : tmpTable.schema()) {
			for (BinaryOp binop : BinaryOp.values()) {
				for (Cell constCell : option.extCells) {
					if (!isTried(col, binop, constCell))
						continue;
					BinaryPred pred = new BinaryPred(col, binop, constCell);
					BitTable result = tmpTable.selection(pred);
					search.addPred(pred, result.rowBits);
				}
			}
		}

		for (ColSchema column : tmpTable.schema()) {
			for (UnaryOp unop : UnaryOp.values()) {
				UnaryPred pred = new UnaryPred(column, unop);
				BitTable result = tmpTable.selection(pred);
				search.addPred(pred, result.rowBits);
			}
		}

		// generate kernel
		search.generateKernel();

		List<RAOperator> ret = new ArrayList<>();
		List<BitRow> rows = search.allRows();
		for (BitRow row : rows) {
			tmpTable.rowBits = row.rowBits;// update the bits directly.

			// pruning by post condition.
			if (!constraint.isPruned(tmpTable, outTable)) {
				Selection clone = target.clone();
				clone.condition = Conjunction.from(row.pred);
				ret.add(clone);
			}
		}

		return ret;
	}

}
