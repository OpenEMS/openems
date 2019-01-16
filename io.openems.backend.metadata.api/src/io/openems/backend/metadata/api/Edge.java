package io.openems.backend.metadata.api;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public class Edge {

	private final Logger log = LoggerFactory.getLogger(Edge.class);

	public enum State {
		ACTIVE, INACTIVE, TEST, INSTALLED_ON_STOCK, OFFLINE;
	}

	private final String apikey;
	private String id;
	private String comment;
	private State state;
	private String version;
	private String producttype;
	private EdgeConfig config;
	private ZonedDateTime lastMessage = null;
	private ZonedDateTime lastUpdate = null;
	private Integer soc = null;
	private String ipv4 = null;
	private boolean isOnline;

	public Edge(String id, String apikey, String comment, State state, String version, String producttype,
			EdgeConfig config, Integer soc, String ipv4) {
		this.id = id;
		this.apikey = apikey;
		this.comment = comment;
		this.state = state;
		this.version = version;
		this.producttype = producttype;
		this.config = config;
		this.soc = soc;
		this.ipv4 = ipv4;
	}

	public String getApikey() {
		return apikey;
	}

	public String getId() {
		return id;
	}

	public String getComment() {
		return comment;
	}

	public EdgeConfig getConfig() {
		return config;
	}

	public String getVersion() {
		return version;
	}

	public String getProducttype() {
		return producttype;
	}

	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("comment", this.comment) //
				.addProperty("version", this.version) //
				.addProperty("producttype", this.producttype) //
				.addProperty("online", this.isOnline) //
				.build();
	}

	@Override
	public String toString() {
		return "Edge [id=" + id + ", comment=" + comment + ", state=" + state + ", version=" + version
				+ ", producttype=" + producttype + ", deprecatedConfig="
				+ (config.toString().isEmpty() ? "NOT_SET" : "set") + ", lastMessage=" + lastMessage + ", lastUpdate="
				+ lastUpdate + ", soc=" + soc + ", ipv4=" + ipv4 + ", isOnline=" + isOnline + "]";
	}

	/*
	 * Online
	 */
	private final List<Consumer<Boolean>> onSetOnline = new CopyOnWriteArrayList<>();

	public void onSetOnline(Consumer<Boolean> listener) {
		this.onSetOnline.add(listener);
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	/**
	 * Marks this Edge as being online. This is called by an event listener.
	 * 
	 * @param isOnline true if the Edge is online
	 */
	public synchronized void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
		this.onSetOnline.forEach(listener -> listener.accept(isOnline));
	}

	/*
	 * Config
	 */
	private final List<Consumer<EdgeConfig>> onSetConfig = new CopyOnWriteArrayList<>();

	public void onSetConfig(Consumer<EdgeConfig> listener) {
		this.onSetConfig.add(listener);
	}

	/**
	 * Sets the configuration for this Edge.
	 * 
	 * @param config the configuration
	 */
	public synchronized void setConfig(EdgeConfig config) {
		this.config = config;
		this.onSetConfig.forEach(listener -> listener.accept(config));
	}

	/*
	 * State
	 */
	public void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

	/*
	 * Last Message
	 */
	private final List<Runnable> onSetLastMessageTimestamp = new CopyOnWriteArrayList<>();

	public void onSetLastMessage(Runnable listener) {
		this.onSetLastMessageTimestamp.add(listener);
	}

	public synchronized void setLastMessageTimestamp() {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastMessage = now;
		this.onSetLastMessageTimestamp.forEach(listener -> listener.run());
	}

	public ZonedDateTime getLastMessageTimestamp() {
		return lastMessage;
	}

	/*
	 * Last Update
	 */
	private final List<Runnable> onSetLastUpdateTimestamp = new CopyOnWriteArrayList<>();

	public void onSetLastUpdate(Runnable listener) {
		this.onSetLastUpdateTimestamp.add(listener);
	}

	public synchronized void setLastUpdateTimestamp() {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastUpdate = now;
		this.onSetLastUpdateTimestamp.forEach(listener -> listener.run());
	}

	public ZonedDateTime getLastUpdateTimestamp() {
		return lastUpdate;
	}

	/*
	 * Version
	 */
	private final List<Consumer<String>> onSetVersion = new CopyOnWriteArrayList<>();

	public void onSetVersion(Consumer<String> listener) {
		this.onSetVersion.add(listener);
	}

	public synchronized void setVersion(String version) {
		if (this.version == null || !version.equals(this.version)) { // on change
			log.info("Edge [" + this.getId() + "]: Update version to [" + version + "]. It was [" + this.version + "]");
			this.version = version;
			this.onSetVersion.forEach(listener -> listener.accept(version));
		}
	}

	/*
	 * State of Charge (SoC)
	 */
	private final List<Consumer<Integer>> onSetSoc = new CopyOnWriteArrayList<>();

	public void onSetSoc(Consumer<Integer> listener) {
		this.onSetSoc.add(listener);
	}

	public synchronized void setSoc(int soc) {
		if (this.soc == null || this.soc.intValue() != soc) { // on change
			log.debug("Edge [" + this.getId() + "]: Update SoC to [" + soc + "]. It was [" + this.soc + "]");
			this.soc = soc;
			this.onSetSoc.forEach(listener -> listener.accept(soc));
		}
	}

	/*
	 * IPv4
	 */
	private final List<Consumer<String>> onSetIpv4 = new CopyOnWriteArrayList<>();

	public void onSetIpv4(Consumer<String> listener) {
		this.onSetIpv4.add(listener);
	}

	public synchronized void setIpv4(String ipv4) {
		if (this.ipv4 == null || !ipv4.equals(this.ipv4)) { // on change
			log.debug("Edge [" + this.getId() + "]: Update IPv4 to [" + ipv4 + "]. It was [" + this.ipv4 + "]");
			this.ipv4 = ipv4;
			this.onSetIpv4.forEach(listener -> listener.accept(ipv4));
		}
	}

}
