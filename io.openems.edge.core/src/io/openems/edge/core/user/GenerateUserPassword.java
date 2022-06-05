package io.openems.edge.core.user;

import java.io.IOException;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import io.openems.common.session.Role;
import io.openems.edge.common.host.DummyHost;

public class GenerateUserPassword {

	/**
	 * Generate FEMS User Passwords.
	 * 
	 * @param args command line arguments
	 * @throws IOException on error
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Call with parameters!");
			System.err.println("./generatePassword fems123 admin");
			System.exit(1);
		}

		String hostname = args[0];
		String username = args[1];
		Role role = Role.getRole(username);

		DummyHost host = new DummyHost().withHostname(hostname);

		String password = UserServiceUtils.generatePassword(host, username, role);

		System.out.println(Splitter.fixedLength(4).splitToStream(password).collect(Collectors.joining(" ")));
	}

}
