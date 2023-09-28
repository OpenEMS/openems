package io.openems.edge.core.host;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemUpdateRequest;
import io.openems.edge.core.host.jsonrpc.GetSystemUpdateStateRequest;
import io.openems.edge.core.host.jsonrpc.GetSystemUpdateStateResponse;
import io.openems.edge.core.host.jsonrpc.GetSystemUpdateStateResponse.UpdateState;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SystemUpdateHandler {

	private static final int SHORT_TIMEOUT = 10; // [s]

	private static final String MARKER_BASH_TRACE = "+-+-+-+ ";
	private static final String MARKER = "#-#-#-# ";
	private static final String MARKER_FINISHED = MARKER + "FINISHED ";
	private static final String MARKER_FINISHED_SUCCESSFULLY = MARKER_FINISHED + "SUCCESSFULLY";
	private static final String MARKER_FINISHED_WITH_ERROR = MARKER_FINISHED + "WITH ERROR";

	private final Logger log = LoggerFactory.getLogger(SystemUpdateHandler.class);
	private final HostImpl parent;
	private final UpdateState updateState = new UpdateState();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	public SystemUpdateHandler(HostImpl parent) {
		this.parent = parent;
	}

	/**
	 * Deactivates the {@link SystemUpdateHandler}.
	 */
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 1);
	}

	/**
	 * Handles a {@link GetSystemUpdateStateRequest}.
	 *
	 * @param request the {@link GetSystemUpdateStateRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleGetSystemUpdateStateRequest(
			GetSystemUpdateStateRequest request) throws OpenemsNamedException {
		final var result = new CompletableFuture<JsonrpcResponseSuccess>();

		if (this.updateState.isRunning()) {
			result.complete(GetSystemUpdateStateResponse.isRunning(request.getId(), this.updateState));

		} else {
			// Read currently installed version
			this.executeSystemCommand("dpkg-query --showformat='${Version}' --show " + OpenemsOEM.SYSTEM_UPDATE_PACKAGE,
					SHORT_TIMEOUT).whenComplete((response, ex) -> {
						if (ex != null) {
							result.completeExceptionally(ex);
							return;
						}
						var stdout = response.getStdout();
						if (stdout.length < 1) {
							result.completeExceptionally(ex /* todo */);
							return;
						}
						var currentVersion = stdout[0];

						// Read latest version
						try {
							var latestVersion = this.download(OpenemsOEM.SYSTEM_UPDATE_LATEST_VERSION_URL).trim();
							result.complete(
									GetSystemUpdateStateResponse.from(request.getId(), currentVersion, latestVersion));

						} catch (IOException e) {
							result.completeExceptionally(e);
							return;
						}
					});
		}
		return result;
	}

	private String download(String url) throws IOException {
		var client = new OkHttpClient();
		var r = new Request.Builder() //
				.url(url) //
				.build();
		try (var resp = client.newCall(r).execute()) {
			if (!resp.isSuccessful()) {
				throw new IOException(resp.message());
			}

			return resp.body().string().trim();
		}
	}

	private CompletableFuture<ExecuteSystemCommandResponse> executeSystemCommand(String command, int timeoutSeconds)
			throws OpenemsNamedException {
		final var runInBackground = false;
		final Optional<String> username = Optional.empty();
		final Optional<String> password = Optional.empty();
		return this.parent.operatingSystem.handleExecuteCommandRequest(
				new ExecuteSystemCommandRequest(command, runInBackground, timeoutSeconds, username, password));
	}

	/**
	 * Handles a {@link ExecuteSystemUpdateRequest} and makes sure the update is
	 * executed only once.
	 *
	 * @param request the {@link ExecuteSystemUpdateRequest}
	 * @return the {@link JsonrpcResponseSuccess}
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleExecuteSystemUpdateRequest(
			ExecuteSystemUpdateRequest request) throws OpenemsNamedException {
		if (this.updateState.isRunning()) {
			throw new OpenemsException("System Update is already running");

		}
		this.updateState.reset();
		this.updateState.setRunning(true);
		this.updateState.setDebugMode(request.isDebug());

		var result = new CompletableFuture<JsonrpcResponseSuccess>();
		this.executor.execute(() -> {
			var response = GetSystemUpdateStateResponse.isRunning(request.getId(), this.updateState);
			try {
				this.executeUpdate(result);
				this.updateState.setPercentCompleted(100);
				this.updateState.addLog("# Finished successfully");
				result.complete(response);

			} catch (Exception e) {
				this.updateState.addLog("# Finished with error");
				this.parent.logError(this.log, "Error while executing System Update: " + e.getMessage());
				e.printStackTrace();
				result.completeExceptionally(new OpenemsException(e.getMessage() + "\n" + response.toString()));
			}
			this.updateState.setRunning(false);
		});
		return result;
	}

	private void executeUpdate(CompletableFuture<JsonrpcResponseSuccess> result) throws Exception {
		Path logFile = null;
		Path scriptFile = null;
		try {
			logFile = Files.createTempFile("system-update-log-", null);
			this.updateState.addLog("# Creating Logfile [" + logFile + "]");

			// Download Update Script to temporary file
			this.updateState.addLog("# Downloading update script [" + OpenemsOEM.SYSTEM_UPDATE_SCRIPT_URL + "]");
			scriptFile = Files.createTempFile("system-update-script-", null);
			var script = //
					"export PS4='" + MARKER_BASH_TRACE + "${LINENO} '; \n" //
							+ this.download(OpenemsOEM.SYSTEM_UPDATE_SCRIPT_URL);
			Files.write(scriptFile, script.getBytes(StandardCharsets.US_ASCII));

			final float totalNumberOfLines = script.split("\r\n|\r|\n").length;

			// Make sure 'at' command is available
			if (this.executeSystemCommand("which at", SHORT_TIMEOUT).get().getStdout().length == 0) {
				this.updateState.addLog("# Command 'at' is missing");

				{
					this.updateState.addLog("# Executing 'apt-get update'");
					var response = this.executeSystemCommand("apt-get update", 3600).get();
					this.updateState.addLog("'apt-get update'", response);
					if (response.getExitCode() != 0) {
						throw new Exception("'apt-get update' failed");
					}
				}
				{
					this.updateState.addLog("# Executing 'apt-get install at'");
					var response = this.executeSystemCommand("apt-get -y install at", 3600).get();
					this.updateState.addLog("'apt-get install at'", response);
					if (response.getExitCode() != 0) {
						throw new Exception("'apt-get install at' failed");
					}
				}
			}

			// Execute Update Script
			{
				this.updateState.addLog("# Executing update script [" + scriptFile + "]");
				var response = this.executeSystemCommand("echo '" //
						+ "  {" //
						+ "    bash -ex " + scriptFile.toString() + "; " //
						+ "    if [ $? -eq 0 ]; then " //
						+ "      echo \"" + MARKER_FINISHED_SUCCESSFULLY + "\"; " //
						+ "    else " //
						+ "      echo \"" + MARKER_FINISHED_WITH_ERROR + "\"; " //
						+ "    fi; " //
						+ "  } >" + logFile.toAbsolutePath() + " 2>&1' " //
						+ "| at now", SHORT_TIMEOUT).get();
				if (response.getExitCode() != 0) {
					throw new Exception("Executing update script [" + scriptFile + "] failed");
				}
			}

			// Read log output
			var keepReading = true;

			try (final var reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(logFile.toFile()), StandardCharsets.ISO_8859_1))) {
				while (keepReading) {
					final var line = reader.readLine();
					if (line == null) {
						// wait until there is more of the file for us to read
						Thread.sleep(500);
						continue;
					}

					final String log;
					if (line.startsWith(MARKER_BASH_TRACE)) {
						/*
						 * Update percent completed + reformat commands
						 */
						var lineWithNumber = line.substring(MARKER_BASH_TRACE.length());
						var lengthOfNumber = lineWithNumber.indexOf(" ");
						// Parse number of line and calculate percent completed
						var numberOfLine = Integer.parseInt(lineWithNumber.substring(0, lengthOfNumber));
						this.updateState.setPercentCompleted(Math.round(numberOfLine * 100 / totalNumberOfLines));
						// Strip number of line and prefix with '#'
						log = "# " + lineWithNumber.substring(lengthOfNumber);

					} else if (line.contains(MARKER_FINISHED)) {
						/*
						 * Finished update script
						 */
						if (line.contains(MARKER_FINISHED_WITH_ERROR)) {
							// Finished with error
							throw new Exception("Error while executing update script");
						}
						// Else: finished successfully
						break;

					} else {
						log = line;
					}

					if (log != null) {
						this.updateState.addLog(log);
					}
				}
			}

		} finally {
			// Cleanup
			if (logFile != null) {
				try {
					Files.delete(logFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (scriptFile != null) {
				try {
					Files.delete(scriptFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
