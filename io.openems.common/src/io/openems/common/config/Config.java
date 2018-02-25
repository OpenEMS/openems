package io.openems.common.config;

import java.util.Hashtable;

public class Config extends Hashtable<String, Object> {

	// private final Logger log = LoggerFactory.getLogger(Config.class);

	private final String pid;

	public Config(String pid) {
		this.pid = pid;
		this.put("service.pid", pid);
	}

	public String getPid() {
		return pid;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public synchronized Object get(Object key) {
		Object o = super.get(key);
		// log.debug("Reading from Config PID [" + this.getPid() + "]: [" + key + "=" +
		// o + "]");
		return o;
	}
}
