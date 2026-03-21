package io.openems.edge.core.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.StringUtils;

final class Bash {

	private static final Logger LOG = LoggerFactory.getLogger(Bash.class);
	private static final ExecutorService EXECUTOR = Executors
			.newThreadPerTaskExecutor(Thread.ofVirtual().name("Bash").factory());

	public static record Command(int exitCode, List<String> stdout, List<String> stderr) {

	}

	private String command;
	private int timeoutSeconds;
	private String password;
	private String username;
	private boolean runInBackground;

	public Bash(String command) {
		this.command = command;
		this.runInBackground = false;
		this.timeoutSeconds = 10;
		this.password = null;
		this.username = null;
	}

	public Bash withTimeout(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
		return this;
	}

	public Bash withSudo(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	public Bash runInBackground(boolean runInBackground) {
		this.runInBackground = runInBackground;
		return this;
	}

	/**
	 * Execute the command and return a future with the result.
	 * 
	 * <p>
	 * If runInBackground is true, the future will be completed immediately with a
	 * message and the actual command will be executed in the
	 * 
	 * @return a future with the result of the command execution
	 */
	public CompletableFuture<Command> execute() {
		final String[] cmd = toBashCommand(this.command, this.username, this.password);
		final int timeout = this.runInBackground ? -1 : this.timeoutSeconds;

		final var future = executeCommand(this.command, cmd, timeout);

		if (!this.runInBackground) {
			return future;
		}

		future.whenComplete((result, error) -> {
			if (error != null) {
				LOG.error("Error executing command [{}]: {}", this.command, error.getMessage());
			} else {
				LOG.info("Command [{}] finished with exit code {}.", this.command, result.exitCode());
			}
		});
		return CompletableFuture.completedFuture(new Command(0, List.of(//
				"Command [" + this.command + "] executed in background...", //
				"Check system logs for more information."), List.of()));
	}

	private static String[] toBashCommand(String command, String username, String password) {
		final List<String> cmd = new ArrayList<>();
		cmd.add("/bin/bash");
		cmd.add("-c");
		cmd.add("--");
		String login = "";

		if (!StringUtils.isNullOrBlank(password)) {
			login = "echo " + password + " | /usr/bin/sudo -Sk -p '' ";
			if (!StringUtils.isNullOrBlank(username)) {
				login += " -u '" + username + "' ";
			}
			login += "-- ";
		}
		cmd.add(login + command);
		return cmd.toArray(new String[cmd.size()]);
	}

	private static CompletableFuture<Bash.Command> executeCommand(String rawCmd, String[] command, int timeoutSeconds) {
		final var future = new CompletableFuture<Bash.Command>();

		EXECUTOR.execute(() -> {
			final Process proc;
			try {
				proc = new ProcessBuilder(command).start();
			} catch (IOException e) {
				future.completeExceptionally(e);
				return;
			}

			Consumer<Throwable> exceptionCallback = e -> {
				LOG.error("Error in Command [{}]: {}", rawCmd, e.getMessage());
				proc.destroy();
			};

			final var stdout = proc.getInputStream();
			final var stderr = proc.getErrorStream();

			final var stdoutFuture = InputStreamToString.supplyAsync(stdout, LOG::info, exceptionCallback);
			final var stderrFuture = InputStreamToString.supplyAsync(stderr, LOG::warn, exceptionCallback);

			try {
				final boolean timedOut;
				if (timeoutSeconds > 0) {
					timedOut = !proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
				} else {
					proc.waitFor();
					timedOut = false;
				}

				if (timedOut) {
					proc.destroy();
					future.completeExceptionally(
							new TimeoutException("Command timed out after " + timeoutSeconds + " seconds"));
					return;
				}

				final var stdoutVal = stdoutFuture.get(1, TimeUnit.SECONDS);
				final var stderrVal = stderrFuture.get(1, TimeUnit.SECONDS);

				future.complete(new Command(proc.exitValue(), stdoutVal, stderrVal));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				future.completeExceptionally(e);
			} catch (Exception e) {
				future.completeExceptionally(e);
			} finally {
				if (proc.isAlive()) {
					proc.destroy();
				}
			}
		});

		return future;
	}

	private static class InputStreamToString implements Supplier<List<String>> {

		private final Consumer<String> lineConsumer;
		private final InputStream stream;
		private final Consumer<Throwable> exceptionCallback;

		public InputStreamToString(InputStream stream, Consumer<String> lineConsumer,
				Consumer<Throwable> exceptionCallback) {
			this.lineConsumer = lineConsumer;
			this.stream = stream;
			this.exceptionCallback = exceptionCallback;
		}

		public static CompletableFuture<List<String>> supplyAsync(InputStream stream, Consumer<String> lineConsumer,
				Consumer<Throwable> exceptionCallback) {
			return CompletableFuture.supplyAsync(new InputStreamToString(stream, lineConsumer, exceptionCallback),
					EXECUTOR);
		}

		@Override
		public List<String> get() {
			var result = new LinkedList<String>();

			try (var inputStream = new InputStreamReader(this.stream); //
					var reader = new BufferedReader(inputStream)) {
				String line;
				while ((line = reader.readLine()) != null) {
					result.add(line);
					this.lineConsumer.accept(line);
				}
			} catch (Exception e) {
				result.add(e.toString());
				this.exceptionCallback.accept(e);
			}

			return result;
		}
	}

}
