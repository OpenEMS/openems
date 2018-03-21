package io.openems.common.config;

import java.util.Hashtable;
import java.util.Optional;

public class Config extends Hashtable<String, Object> {

	// private final Logger log = LoggerFactory.getLogger(Config.class);

	private final String pid;
	private final Optional<String> idOpt;
	private final boolean doNotStore;

	public Config(String pid) {
		this(pid, null, false);
	}

	public Config(String pid, String id) {
		this(pid, id, false);
	}

	public Config(String pid, boolean doNotStore) {
		this(pid, null, doNotStore);
	}

	public Config(String pid, String id, boolean doNotStore) {
		this.pid = pid;
		this.idOpt = Optional.ofNullable(id);
		this.doNotStore = doNotStore;
		this.put("service.pid", pid);
	}

	public String getPid() {
		return this.pid;
	}

	public Optional<String> getIdOpt() {
		return this.idOpt;
	}

	public boolean isDoNotStore() {
		return this.doNotStore;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public synchronized Object get(Object key) {
		Object o = super.get(key);
		return o;
	}
}
