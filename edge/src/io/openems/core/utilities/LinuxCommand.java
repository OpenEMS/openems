package io.openems.core.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.OpenemsException;

public class LinuxCommand {

	private static Logger log = LoggerFactory.getLogger(LinuxCommand.class);

	public static String execute(String password, String command) throws OpenemsException {

		log.info("Executing command [" + command + "]");

		BufferedReader stdin = null;
		BufferedWriter stdout = null;
		BufferedReader stderr = null;

		try {
			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c",
					"echo " + password + " | /usr/bin/sudo -Sk -p '' -- /bin/bash -c -- '" + command + "' 2>&1" });
			// TODO enfoce password when already running as root
			// this complex argument tries to enforce the sudo password and to avoid security vulnerabilites.
			stdout = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
			stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			stdin = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line = null;

			StringBuilder builder = new StringBuilder();
			builder.append("output:\n");
			builder.append("-------\n");
			while ((line = stdin.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}

			builder.append("\n");

			builder.append("error:\n");
			builder.append("------\n");
			while ((line = stderr.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}

			if (!proc.waitFor(30, TimeUnit.SECONDS)) {
				proc.destroy();
				throw new OpenemsException("Command [" + command + "] timed out.");
			}
			return builder.toString();
		} catch (IOException | InterruptedException e) {
			throw new OpenemsException("Unable to execute command [" + command + "]: " + e.getMessage());
		} finally {
			try {
				if (stdin != null) {
					stdin.close();
				}
				if (stdout != null) {
					stdout.close();
				}
				if (stderr != null) {
					stderr.close();
				}
			} catch (IOException e) {
				/* ignore */
			}
		}
	}
}
