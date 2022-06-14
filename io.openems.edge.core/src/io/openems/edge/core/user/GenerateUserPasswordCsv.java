package io.openems.edge.core.user;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import io.openems.common.session.Role;
import io.openems.edge.common.host.DummyHost;

public class GenerateUserPasswordCsv {

	/**
	 * Generate FEMS User Passwords.
	 * 
	 * @param args command line arguments
	 * @throws IOException on error
	 */
	public static void main(String[] args) throws IOException {
		final String username = "admin";
		final Role role = Role.getRole(username);
		DummyHost host = new DummyHost();

		BufferedWriter writer = new BufferedWriter(new FileWriter("passwords-" + username + ".csv"));
		writer.write("FEMS-Nummer;Passwort\n");
		for (int i = 1; i < 4000; i++) {
			String hostname = "fems" + i;
			host.withHostname(hostname);
			String password = UserServiceUtils.generatePassword(host, username, role);
			writer.write(hostname + ";"
					+ Splitter.fixedLength(4).splitToStream(password).collect(Collectors.joining(" ")) + "\n");
		}
		writer.close();

	}

}
