package io.openems.common.config;

import java.util.Hashtable;

public class Config extends Hashtable<String, Object> {

	// private final Logger log = LoggerFactory.getLogger(Config.class);

	private final String pid;
	private final boolean doNotStore;

	public Config(String pid) {
		this(pid, false);
	}

	public Config(String pid, boolean doNotStore) {
		this.pid = pid;
		this.doNotStore = doNotStore;
		this.put("service.pid", pid);
	}

	public String getPid() {
		return pid;
	}

	public boolean isDoNotStore() {
		return doNotStore;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public synchronized Object get(Object key) {
		Object o = super.get(key);
		return o;
	}
}
