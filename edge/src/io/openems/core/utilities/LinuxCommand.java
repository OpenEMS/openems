package io.openems.core.utilities;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxCommand {

	private static Logger log = LoggerFactory.getLogger(LinuxCommand.class);

	/**
	 *
	 * @param password
	 * @param command
	 * @param background
	 *            Execute the command in background
	 * @param timeoutSeconds
	 * @return
	 */
	public static String execute(String password, String command, boolean background, int timeoutSeconds) {

		log.info("Executing command [" + command + "]");
		StringBuilder builder = new StringBuilder();

		try {
			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c",
					"echo " + password + " | /usr/bin/sudo -Sk -p '' -- /bin/bash -c -- '" + command + "' 2>&1" });
			// TODO enfoce password when already running as root
			// this complex argument tries to enforce the sudo password and to avoid security vulnerabilites.

			// get stdout and stderr
			ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(2);
			Future<String> stdout = newFixedThreadPool.submit(new InputStreamToString(command, proc.getInputStream()));
			Future<String> stderr = newFixedThreadPool.submit(new InputStreamToString(command, proc.getErrorStream()));
			newFixedThreadPool.shutdown();

			if(background) {
				builder.append("Command [" + command + "] executed in background...\n");
				builder.append("Check system logs for more information.\n");

			} else {
				// apply command timeout
				if (!proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
					String error = "Command [" + command + "] timed out.";
					builder.append(error);
					builder.append("\n");
					builder.append("\n");
					proc.destroy();
				}

				builder.append("output:\n");
				builder.append("-------\n");
				builder.append(stdout.get(timeoutSeconds, TimeUnit.SECONDS));
				builder.append("\n");
				builder.append("error:\n");
				builder.append("------\n");
				builder.append(stderr.get(timeoutSeconds, TimeUnit.SECONDS));
			}
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			builder.append("\n");
			builder.append("exception:\n");
			builder.append("----------\n");
			builder.append("Error while executing command [" + command + "]: " + e.getClass().getName() + " - "
					+ e.getMessage());
		}
		return builder.toString();
	}
}