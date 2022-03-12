package io.openems.backend.common.metadata;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class Edge {

	private final Logger log = LoggerFactory.getLogger(Edge.class);

	public enum State {
		ACTIVE, INACTIVE, TEST, INSTALLED_ON_STOCK, OFFLINE;
	}

	private final String id;
	private String comment;
	private State state;
	private SemanticVersion version;
	private String producttype;
	private Level sumState;
	private EdgeConfig config;
	private ZonedDateTime lastMessage = null;
	private ZonedDateTime lastUpdate = null;
	private boolean isOnline = false;

	public Edge(String id, String comment, State state, String version, String producttype, Level sumState,
			EdgeConfig config) {
		this.id = id;
		this.comment = comment;
		this.state = state;
		this.version = SemanticVersion.fromStringOrZero(version);
		this.producttype = producttype;
		this.sumState = sumState;
		this.config = config;
	}

	public String getId() {
		return this.id;
	}

	/*
	 * Comment
	 */
	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public EdgeConfig getConfig() {
		return this.config;
	}

	/**
	 * Gets this {@link Edge} as {@link JsonObject}.
	 *
	 * @return a {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("comment", this.comment) //
				.addProperty("version", this.version.toString()) //
				.addProperty("producttype", this.producttype) //
				.addProperty("online", this.isOnline) //
				.build();
	}

	@Override
	public String toString() {
		return "Edge [" //
				+ "id=" + this.id + ", " //
				+ "comment=" + this.comment + ", " //
				+ "state=" + this.state + ", " //
				+ "version=" + this.version + ", " //
				+ "producttype=" + this.producttype + ", " //
				+ "deprecatedConfig=" + (this.config.toString().isEmpty() ? "NOT_SET" : "set") + ", " //
				+ "lastMessage=" + this.lastMessage + ", " //
				+ "lastUpdate=" + this.lastUpdate + ", " //
				+ "isOnline=" + this.isOnline //
				+ "]";
	}

	/*
	 * Online
	 */
	private final List<Consumer<Boolean>> onSetOnline = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Online events.
	 *
	 * @param listener the listener
	 */
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
		this.onSetOnline.forEach(listener -> listener.accept(isOnline));
		this.isOnline = isOnline;
	}

	/*
	 * Config
	 */
	private final List<Consumer<EdgeConfig>> onSetConfig = new CopyOnWriteArrayList<>();

	/**
	 * Adds a listener for reception of new EdgeConfig. The listener is called
	 * before the new config is applied.
	 *
	 * @param listener the Listener
	 */
	public void onSetConfig(Consumer<EdgeConfig> listener) {
		this.onSetConfig.add(listener);
	}

	/**
	 * Sets the configuration for this Edge and calls the SetConfig-Listeners.
	 *
	 * @param config the configuration
	 */
	public synchronized void setConfig(EdgeConfig config) {
		this.setConfig(config, true);
	}

	/**
	 * Sets the configuration for this Edge.
	 *
	 * @param config        the configuration
	 * @param callListeners whether to call the SetConfig-Listeners
	 */
	public synchronized void setConfig(EdgeConfig config, boolean callListeners) {
		if (callListeners) {
			this.onSetConfig.forEach(listener -> listener.accept(config));
		}
		this.config = config;
	}

	/*
	 * State
	 */
	public void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return this.state;
	}

	/*
	 * Last Message
	 */
	private final List<Runnable> onSetLastMessageTimestamp = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Last-Message events.
	 *
	 * @param listener the listener
	 */
	public void onSetLastMessage(Runnable listener) {
		this.onSetLastMessageTimestamp.add(listener);
	}

	/**
	 * Sets the Last-Message-Timestamp and calls the SetLastMessage-Listeners.
	 */
	public synchronized void setLastMessageTimestamp() {
		this.setLastMessageTimestamp(true);
	}

	/**
	 * Sets the Last-Message-Timestamp.
	 *
	 * @param callListeners whether to call the SetLastMessage-Listeners
	 */
	public synchronized void setLastMessageTimestamp(boolean callListeners) {
		if (callListeners) {
			this.onSetLastMessageTimestamp.forEach(Runnable::run);
		}
		var now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastMessage = now;
	}

	public ZonedDateTime getLastMessageTimestamp() {
		return this.lastMessage;
	}

	/*
	 * Last Update
	 */
	private final List<Runnable> onSetLastUpdateTimestamp = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Last-Update events.
	 *
	 * @param listener the listener
	 */
	public void onSetLastUpdate(Runnable listener) {
		this.onSetLastUpdateTimestamp.add(listener);
	}

	/**
	 * Sets the Last-Message-Timestamp and calls the SetLastUpdate-Listeners.
	 */
	public synchronized void setLastUpdateTimestamp() {
		this.setLastUpdateTimestamp(true);
	}

	/**
	 * Sets the Last-Update-Timestamp.
	 *
	 * @param callListeners whether to call the SetLastUpdate-Listeners
	 */
	public synchronized void setLastUpdateTimestamp(boolean callListeners) {
		if (callListeners) {
			this.onSetLastUpdateTimestamp.forEach(Runnable::run);
		}
		var now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastUpdate = now;
	}

	public ZonedDateTime getLastUpdateTimestamp() {
		return this.lastUpdate;
	}

	/*
	 * Version
	 */
	public SemanticVersion getVersion() {
		return this.version;
	}

	private final List<Consumer<SemanticVersion>> onSetVersion = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Version events.
	 *
	 * @param listener the listener
	 */
	public void onSetVersion(Consumer<SemanticVersion> listener) {
		this.onSetVersion.add(listener);
	}

	/**
	 * Sets the version and calls the SetVersion-Listeners.
	 *
	 * @param version the version
	 */
	public synchronized void setVersion(SemanticVersion version) {
		this.setVersion(version, true);
	}

	/**
	 * Sets the version.
	 *
	 * @param version       the version
	 * @param callListeners whether to call the SetVersion-Listeners
	 */
	public synchronized void setVersion(SemanticVersion version, boolean callListeners) {
		if (!Objects.equal(this.version, version)) { // on change
			if (callListeners) {
				this.log.info("Edge [" + this.getId() + "]: Update version to [" + version + "]. It was ["
						+ this.version + "]");
				this.onSetVersion.forEach(listener -> listener.accept(version));
			}
			this.version = version;
		}
	}

	/*
	 * Producttype
	 */
	public String getProducttype() {
		return this.producttype;
	}

	private final List<Consumer<String>> onSetProducttype = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Product-Type events.
	 *
	 * @param listener the listener
	 */
	public void onSetProducttype(Consumer<String> listener) {
		this.onSetProducttype.add(listener);
	}

	/**
	 * Sets the Producttype and calls the SetProducttype-Listeners.
	 *
	 * @param producttype the Producttype
	 */
	public synchronized void setProducttype(String producttype) {
		this.setProducttype(producttype, true);
	}

	/**
	 * Sets the Producttype.
	 *
	 * @param producttype   the Producttype
	 * @param callListeners whether to call the SetProducttype-Listeners
	 */
	public synchronized void setProducttype(String producttype, boolean callListeners) {
		if (!Objects.equal(this.producttype, producttype)) { // on change
			if (callListeners) {
				this.log.info("Edge [" + this.getId() + "]: Update Product-Type to [" + producttype + "]. It was ["
						+ this.producttype + "]");
				this.onSetProducttype.forEach(listener -> listener.accept(producttype));
			}
			this.producttype = producttype;
		}
	}

	/*
	 * Sum-State (value of channel "_sum/State").
	 */
	public Level getSumState() {
		return this.sumState;
	}

	private final List<Consumer<Level>> onSetSumState = new CopyOnWriteArrayList<>();

	/**
	 * Add a Listener for Set-Sum-State events.
	 *
	 * @param listener the listener
	 */
	public void onSetSumState(Consumer<Level> listener) {
		this.onSetSumState.add(listener);
	}

	/**
	 * Sets the sumState and calls the SetSumState-Listeners.
	 *
	 * @param sumState the sumState
	 */
	public synchronized void setSumState(Level sumState) {
		this.setSumState(sumState, true);
	}

	/**
	 * Sets the version.
	 *
	 * @param sumState      the sumState
	 * @param callListeners whether to call the SetSumState-Listeners
	 */
	public synchronized void setSumState(Level sumState, boolean callListeners) {
		if (!Objects.equal(this.sumState, sumState)) { // on change
			if (callListeners) {
				this.onSetSumState.forEach(listener -> listener.accept(sumState));
			}
			this.sumState = sumState;
		}
	}

}
