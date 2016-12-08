package io.openems.api.security;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Authentication {
	private final static Logger log = LoggerFactory.getLogger(Authentication.class);

	/*
	 * Singleton
	 */
	private static Authentication instance;

	public static synchronized Authentication getInstance() {
		if (Authentication.instance == null) {
			Authentication.instance = new Authentication();
		}
		return Authentication.instance;
	}

	/*
	 * This class
	 */
	private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

	private Authentication() {}

	public Session byUserPassword(String username, String password) {
		User user = User.authenticate(username, password);
		Session session = addSession(user);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated. " + //
					"Created session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by username[" + username + "] + password failed.");
		}
		return session;
	}

	public Session byPassword(String password) {
		User user = User.authenticate(password);
		Session session = addSession(user);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated. " + //
					"Created session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by password failed.");
		}
		return session;
	}

	public Session bySession(String token) {
		Session session = sessions.get(token);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated by " + //
					"session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by session failed.");
		}
		return session;
	}

	private Session addSession(User user) {
		if (user != null) {
			Session session = new Session(user);
			sessions.put(session.getToken(), session);
			return session;
		}
		return null;
	}
}
