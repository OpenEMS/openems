package io.openems.backend.metadata.api;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

public class Edge {
	public enum State {
		ACTIVE, INACTIVE, TEST, INSTALLED_ON_STOCK, OFFLINE;
	}

	private final int id;
	private String name;
	private String comment;
	private State state;
	private String version;
	private String producttype;
	private JsonObject jConfig;
	private ZonedDateTime lastMessage = null;
	private ZonedDateTime lastUpdate = null;
	private Integer soc = null;
	private String ipv4 = null;
	private boolean isOnline;

	public Edge(int id, String name, String comment, State state, String version, String producttype,
			JsonObject jConfig) {
		this.id = id;
		this.name = name;
		this.comment = comment;
		this.state = state;
		this.version = version;
		this.producttype = producttype;
		this.jConfig = jConfig;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public JsonObject getConfig() {
		return this.jConfig;
	}

	public String getVersion() {
		return version;
	}

	public String getProducttype() {
		return producttype;
	}

	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("id", this.id);
		j.addProperty("name", this.name);
		j.addProperty("comment", this.comment);
		j.addProperty("version", this.version);
		j.addProperty("producttype", this.producttype);
		j.addProperty("online", this.isOnline);
		return j;
	}

	@Override
	public String toString() {
		return "Edge [id=" + id + ", name=" + name + ", comment=" + comment + ", producttype=" + producttype
				+ ", isOnline=" + isOnline + "]";
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
	 */
	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
		this.onSetOnline.forEach(listener -> listener.accept(isOnline));
	}

	/*
	 * Config
	 */
	private final List<Consumer<JsonObject>> onSetConfig = new CopyOnWriteArrayList<>();

	public void onSetConfig(Consumer<JsonObject> listener) {
		this.onSetConfig.add(listener);
	}

	public void setConfig(JsonObject jConfig) {
		if (!jConfig.equals(this.jConfig)) { // on change
			this.jConfig = jConfig;
			this.onSetConfig.forEach(listener -> listener.accept(jConfig));
		}
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
	private final List<Runnable> onSetLastMessage = new CopyOnWriteArrayList<>();

	public void onSetLastMessage(Runnable listener) {
		this.onSetLastMessage.add(listener);
	}

	public void setLastMessage() {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastMessage = now;
		this.onSetLastMessage.forEach(listener -> listener.run());
	}

	public ZonedDateTime getLastMessage() {
		return lastMessage;
	}

	/*
	 * Last Update
	 */
	private final List<Runnable> onSetLastUpdate = new CopyOnWriteArrayList<>();

	public void onSetLastUpdate(Runnable listener) {
		this.onSetLastUpdate.add(listener);
	}

	public void setLastUpdate() {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastUpdate = now;
		this.onSetLastUpdate.forEach(listener -> listener.run());
	}

	public ZonedDateTime getLastUpdate() {
		return lastUpdate;
	}

	/*
	 * Version
	 */
	private final List<Consumer<String>> onSetVersion = new CopyOnWriteArrayList<>();

	public void onSetVersion(Consumer<String> listener) {
		this.onSetVersion.add(listener);
	}

	public void setVersion(String version) {
		if (!version.equals(this.version)) { // on change
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

	public void setSoc(int soc) {
		if (Integer.valueOf(soc) != this.soc) { // on change
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

	public void setIpv4(String ipv4) {
		if (!ipv4.equals(this.ipv4)) { // on change
			this.ipv4 = ipv4;
			this.onSetIpv4.forEach(listener -> listener.accept(ipv4));
		}
	}
}
