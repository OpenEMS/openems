package io.openems.edge.core.user;

import io.openems.common.session.Role;
import io.openems.edge.common.host.DummyHost;

public class GenerateUserPassword {

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

		System.out.println(UserServiceUtils.generatePassword(host, username, role));
	}

}
