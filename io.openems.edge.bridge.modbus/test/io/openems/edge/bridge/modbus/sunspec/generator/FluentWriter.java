package io.openems.edge.bridge.modbus.sunspec.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class FluentWriter implements AutoCloseable {

	private final BufferedWriter w;

	/**
	 * Builds a {@link FluentWriter}.
	 * 
	 * @param path the path
	 * @return a {@link FluentWriter}
	 * @throws IOException on error
	 */
	public static FluentWriter to(String path) throws IOException {
		return new FluentWriter(Paths.get(path));
	}

	private FluentWriter(Path path) throws IOException {
		this.w = Files.newBufferedWriter(path);
	}

	/**
	 * Writes line without final newLine.
	 * 
	 * @param line the line
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter write(String line) throws IOException {
		this.w.write(line);
		return this;
	}

	/**
	 * Writes text without final newLine if predicate is true.
	 * 
	 * @param predicate the predicate; if true write text
	 * @param text      the text Supplier
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter writeIf(boolean predicate, Supplier<String> text) throws IOException {
		if (predicate) {
			this.write(text.get());
		}
		return this;
	}

	/**
	 * Writes true- or false-text without final newLine.
	 * 
	 * @param predicate the predicate; if true write true-line; if false write
	 *                  false-line
	 * @param trueText  the true-text
	 * @param falseText the false-text
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter writeIf(boolean predicate, String trueText, String falseText) throws IOException {
		return predicate //
				? this.write(trueText) //
				: this.write(falseText);
	}

	/**
	 * Writes line with final newLine.
	 * 
	 * @param line the line
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter writeln(String line) throws IOException {
		this.w.write(line);
		this.w.newLine();
		return this;
	}

	/**
	 * Writes line if predicate is true.
	 * 
	 * @param predicate the predicate; if true write line
	 * @param line      the line
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter writelnIf(boolean predicate, String line) throws IOException {
		if (predicate) {
			this.writeln(line);
		}
		return this;
	}

	/**
	 * Writes true- or false-line without final newLine.
	 * 
	 * @param predicate the predicate; if true write true-line; if false write
	 *                  false-line
	 * @param trueLine  the true-line
	 * @param falseLine the false-line
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter writelnIf(boolean predicate, String trueLine, String falseLine) throws IOException {
		return predicate //
				? this.writeln(trueLine) //
				: this.writeln(falseLine);
	}

	/**
	 * Writes an empty newLine.
	 * 
	 * @return myself
	 * @throws IOException on error
	 */
	public FluentWriter blank() throws IOException {
		this.w.newLine();
		return this;
	}

	@Override
	public void close() throws IOException {
		this.w.close();
	}
}