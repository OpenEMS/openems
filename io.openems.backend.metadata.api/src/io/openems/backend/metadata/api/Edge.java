package io.openems.backend.metadata.api;

import java.util.Optional;

import com.google.gson.JsonObject;

public class Edge {
	private final int id;
	private String name;
	private String comment;
	private String producttype;
	private JsonObject jConfig;
	private boolean isOnline;

	public Edge(int id, String name, String comment, String producttype, JsonObject jConfig) {
		this.id = id;
		this.name = name;
		this.comment = comment;
		this.producttype = producttype;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/*
	 * Marks this Edge as being online. This is called by an event listener.
	 */
	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
		// TODO call OnSetOnline
	}

	private Optional<OnSetConfig> onSetConfig = Optional.empty();

	public void onSetConfig(OnSetConfig listener) {
		this.onSetConfig = Optional.of(listener);
	}

	public void setConfig(JsonObject jConfig) {
		this.jConfig = jConfig;
		if (this.onSetConfig.isPresent()) {
			this.onSetConfig.get().call(jConfig);
		}
	}

	public JsonObject getConfig() {
		return this.jConfig;
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("id", this.id);
		j.addProperty("name", this.name);
		j.addProperty("comment", this.comment);
		j.addProperty("producttype", this.producttype);
		j.addProperty("online", this.isOnline);
		return j;
	}

	@Override
	public String toString() {
		return "Edge [id=" + id + ", name=" + name + ", comment=" + comment + ", producttype=" + producttype
				+ ", isOnline=" + isOnline + "]";
	}
}
