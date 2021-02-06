package patsql.ra.util;

import patsql.entity.synth.Example;
import patsql.entity.synth.NamedTable;
import patsql.entity.synth.SynthOption;
import patsql.entity.table.Cell;
import patsql.entity.table.ColSchema;
import patsql.entity.table.Table;
import patsql.entity.table.Type;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class EvaluationUtil {

	public static void writeResultToFile(File file, String sql, long synthesisTime, Example ex, SynthOption opt) {
		synthesisTime = (synthesisTime == 0) ? 1 : synthesisTime;

		String name = file.getParentFile().getName() + "_" + file.getName();
		name = name.replace(".md", "");
		if (sql == null)
			name = name + "X";

		StringBuilder sb = new StringBuilder();

		String description = "(nothing)";
		try (Stream<String> lines1 = Files.lines(file.toPath(), StandardCharsets.UTF_8);
			 Stream<String> lines2 = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
			//noinspection DynamicRegexReplaceableByCompiledPattern
			description = lines1
					.map(line -> line.replaceAll(" ", ""))
					.filter(line -> line.startsWith("//http"))
					.map(line -> line.replaceFirst("//", ""))
					.map(line -> "<a href=\"" + line + "\" target=\"blank\"> URL </a>")
					.collect(Collectors.joining("<br>"))
					+ "<br>"
					+ lines2
					.filter(line -> line.startsWith("//"))
					.filter(line -> !line.replaceAll(" ", "").startsWith("//http"))
					.map(line -> line.replaceFirst("//", ""))
					.map(line -> "<span>" + line + "</span>")
					.collect(Collectors.joining("<br>"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		sb.append("<h2>Description</h2>").append('\n');
		sb.append(description);

		String header = """
				<!DOCTYPE html>
				<html lang="en">
				    
				<head>
				    <meta charset="utf-8" />
				    <meta name="viewport" content="width=device-width, initial-scale=1.0">
				    <title>%s</title>
				    <link rel="stylesheet" href="../css/result.css"/>
				    <link rel="stylesheet" href="../css/idea.css"/>
				    <script type="text/javascript" src="../js/lib/highlight.pack.js"></script>
				    <script>hljs.initHighlightingOnLoad();</script>
				</head>
				""".formatted(name);
		sb.append(header);

		String ioExampleHeading = """
				<body>
				<h2>I/O Example</h2>
				""";
		sb.append(ioExampleHeading);

		for (NamedTable inTbl : ex.inputs) {
			String inputHeader = """
					<h3>INPUT: %s</h3>
										
					<table class="in_table mono">
					""".formatted(inTbl.name);
			sb.append(inputHeader);
			loadTable(sb, inTbl.table);
			sb.append("</table>").append('\n');
		}

		String outputHeader = """
				<h3>OUTPUT</h3>
				<table class="out_table mono">
				""";
		sb.append(outputHeader);
		loadTable(sb, ex.output);
		sb.append("</table>").append('\n');

		String hintsHeader = """
				<h3>Hints</h3>
				<div>
				    <ul class"mono">
				""";
		sb.append(hintsHeader);
		if (opt.extCells.length == 0) {
			sb.append("<li>(empty)</li>");
		} else {
			for (Cell ext : opt.extCells) {
				String hint = """
						        <li>%s<span class="type">:%s</span></li>
						""".formatted(ext.value(), ext.type());
				sb.append(hint);
			}
		}
		String hintsFooter = """
				    </ul>
				</div>
				""";
		sb.append(hintsFooter);

		String solutionHeader = "<h2>Our Solution</h2>";
		sb.append(solutionHeader).append('\n');
		if (sql != null) {
			String solution = """
					<pre><code class="sql">
					%s
					</code></pre>
					""".formatted(sql);
			sb.append(solution);
		} else {
			sb.append("(fail)").append('\n');
		}

		String ms = sql == null ? "(timeout)" : Long.toString(synthesisTime);
		String time = """
				<h2>Synthesis Time</h2>
				<div>%s milliseconds</div>
				""".formatted(ms);
		sb.append(time);

		String outputFooter = """
				</body>
				</html>
				""";
		sb.append(outputFooter);

		try {
			Files.writeString(Path.of("html", "results", name + ".html"), sb, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadTable(StringBuilder sb, Table table) {
		String indent = " ".repeat(4);

		sb.append(indent).append("<tr>");
		for (ColSchema sc : table.schema())
			sb.append(String.format("<th>%s<span class=\"type\">:%s</span></th>", sc.name, sc.type));
		sb.append("</tr>").append('\n');

		for (int i = 0; i < table.height(); i++) {
			sb.append(indent).append("<tr>");
			for (Cell c : table.row(i)) {
				String value = c.type() != Type.Null ?
						c.value().trim() : String.format("<span class= \"null\">%s</span>", c.value());
				sb.append("<td>").append(value).append("</td>");
			}
			sb.append("</tr>").append('\n');
		}
	}
}
