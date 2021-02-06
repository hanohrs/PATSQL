package patsql.synth;

import org.jetbrains.annotations.Nullable;
import patsql.entity.synth.Example;
import patsql.entity.synth.SynthOption;
import patsql.entity.table.Cell;
import patsql.entity.table.Table;
import patsql.entity.table.Type;
import patsql.ra.operator.BaseTable;
import patsql.ra.operator.RA;
import patsql.ra.operator.RAOperator;
import patsql.ra.util.RAOptimizer;
import patsql.ra.util.RAUtils;
import patsql.ra.util.Utils;
import patsql.synth.filler.SketchFiller;
import patsql.synth.sketcher.Sketcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RASynthesizer implements Callable<RAOperator> {
	final Example example;
	final SynthOption option;

	public RASynthesizer(Example example, SynthOption option) {
		this.example = example;
		this.option = option;
	}

	@Nullable
	private Boolean memo = null;

	private static boolean hasRAOperator(RAOperator sketch, RA ra) {
		boolean[] ret = new boolean[1]; // ret[0] == false
		RAUtils.traverse(sketch, op -> {
			if (op.kind == ra) {
				ret[0] = true;
				return false;
			}
			return true;
		});
		return ret[0];
	}

	@Override
	public RAOperator call() {
		return synthesize();
	}

	private boolean check(RAOperator program) {
		Table result = program.eval(example.tableEnv());
		return result.hasSameRows(example.output);
	}

	/**
	 * @return null if timeout happens.
	 */
	public @Nullable RAOperator synthesize(int timeoutMs) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<RAOperator> future = service.submit(new RASynthesizer(example, option));
		try {
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			if (Debug.isDebugMode) {
				System.out.println("SYNTHESIS TIME OUT");
			}
			future.cancel(true); // stops the running synthesis
			return null;
		}
	}

	public @Nullable RAOperator synthesize() {
		long startDebug = System.nanoTime();

		boolean isOutputSorted = Arrays.stream(example.output.columns) //
				.map(col -> col.schema) //
				.anyMatch(schema -> example.output.isIncreasing(schema) || example.output.isDecreasing(schema));

		Sketcher sketcher = new Sketcher(example.inputs.length, isOutputSorted);

		for (RAOperator s : sketcher) {
			for (RAOperator sketch : assignNamesOnBaseTables(s)) {
				// check the timeout of itself.
				if (Thread.currentThread().isInterrupted()) {
					return null;
				}

				if (!isValidSketch(sketch)) {
					continue;
				}
				if (Debug.isDebugMode) {
					RAUtils.printSketch(sketch);
				}
				SketchFiller filler = new SketchFiller(sketch, example, option);
				for (RAOperator program : filler.fillSketch()) {
					if (!check(program))
						continue;

					// optimize the program returned.
					program = RAOptimizer.optimize(program);
					if (Debug.isDebugMode) {
						long dur = (System.nanoTime() - startDebug) / 1000000;
						Debug.Time.doneSynth(dur);
					}
					return program;
				}
			}
		}
		return null;
	}

	private boolean isValidSketch(RAOperator sketch) {
		// if external constants exist, selection is needed.
		if (option.extCells.length > 0 && !hasRAOperator(sketch, RA.SELECTION))
			return false;

		// if external constants do not exist, either NULL or left join is needed.
		if (option.extCells.length == 0 && hasRAOperator(sketch, RA.SELECTION))
			return inputContainsNull() || hasRAOperator(sketch, RA.LEFTJOIN);

		return true;
	}

	private boolean inputContainsNull() {
		if (memo != null) {
			return memo;
		}
		if (Arrays.stream(example.inputs)
				.flatMap(in -> Arrays.stream(in.table.columns))
				.flatMap(col -> Arrays.stream(col.cells()))
				.map(Cell::type)
				.anyMatch(Type.Null::equals)) {
			memo = true;
			return true;
		} else {
			memo = false;
			return false;
		}
	}

	private List<RAOperator> assignNamesOnBaseTables(RAOperator sketch) {
		// collect base tables.
		List<BaseTable> baseTables = new ArrayList<>();
		RAUtils.traverse(sketch, op -> {
			if (op.kind == RA.BASETABLE) {
				baseTables.add((BaseTable) op);
			}
			return true;
		});
		Collections.reverse(baseTables);

		// assign names on base tables.
		List<RAOperator> ret = new ArrayList<>();
		for (Assignment assignment : validAssignments(baseTables.size())) {
			RAOperator filled = sketch;
			for (int i = 0; i < baseTables.size(); i++) {
				BaseTable target = baseTables.get(i);
				BaseTable inserted = target.clone();
				inserted.tableId = assignment.names.get(i);
				// fill one ID by updating.
				filled = RAUtils.replace(filled, inserted, target);
			}
			ret.add(filled);
		}
		return ret;
	}

	private List<Assignment> validAssignments(int size) {
		List<List<String>> namesList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			List<String> names = Arrays.asList(example.inputTableNames());
			namesList.add(names);
		}

		List<Assignment> ret = new ArrayList<>();
		for (List<String> a : Utils.cartesianProduct(namesList)) {
			Assignment assign = new Assignment(a);
			if (assign.isValidAssignment(example)) {
				ret.add(assign);
			}
		}
		return ret;
	}

	private record Assignment(List<String> names) {
		private boolean isValidAssignment(Example example) {
			// all IDs must be used at least once.
			return new HashSet<>(names).size() == example.inputs.length;
		}
	}
}
