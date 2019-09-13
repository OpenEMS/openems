package io.openems.backend.metadata.api;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;
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
	private EdgeConfig config;
	private ZonedDateTime lastMessage = null;
	private ZonedDateTime lastUpdate = null;
	private Integer soc = null;
	private String ipv4 = null;
	private Level sumState = null;
	private boolean isOnline = false;

	public Edge(String id, String comment, State state, String version, String producttype, EdgeConfig config,
			Integer soc, String ipv4, Level sumState) {
		this.id = id;
		this.comment = comment;
		this.state = state;
		this.version = SemanticVersion.fromStringOrZero(version);
		this.producttype = producttype;
		this.config = config;
		this.soc = soc;
		this.ipv4 = ipv4;
		this.sumState = sumState;
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

	public String getProducttype() {
		return this.producttype;
	}

	public void setProducttype(String producttype) {
		this.producttype = producttype;
	}

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
			this.onSetLastMessageTimestamp.forEach(listener -> listener.run());
		}
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastMessage = now;
	}

	public ZonedDateTime getLastMessageTimestamp() {
		return this.lastMessage;
	}

	/*
	 * Last Update
	 */
	private final List<Runnable> onSetLastUpdateTimestamp = new CopyOnWriteArrayList<>();

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
			this.onSetLastUpdateTimestamp.forEach(listener -> listener.run());
		}
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
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
	 * State of Charge (SoC)
	 */
	private final List<Consumer<Integer>> onSetSoc = new CopyOnWriteArrayList<>();

	public void onSetSoc(Consumer<Integer> listener) {
		this.onSetSoc.add(listener);
	}

	/**
	 * Sets the State-of-Charge and calls the SetSoc-Listeners.
	 * 
	 * @param soc the State-of-Charge
	 */
	public synchronized void setSoc(Integer soc) {
		this.setSoc(soc, true);
	}

	/**
	 * Sets the State-of-Charge.
	 * 
	 * @param soc           the soc
	 * @param callListeners whether to call the SetSoc-Listeners
	 */
	public synchronized void setSoc(Integer soc, boolean callListeners) {
		if (!Objects.equal(this.soc, soc)) { // on change
			if (callListeners) {
				this.onSetSoc.forEach(listener -> listener.accept(soc));
			}
			this.soc = soc;
		}
	}

	/*
	 * IPv4
	 */
	private final List<Consumer<String>> onSetIpv4 = new CopyOnWriteArrayList<>();

	public void onSetIpv4(Consumer<String> listener) {
		this.onSetIpv4.add(listener);
	}

	/**
	 * Sets the IPv4 address and calls the SetIpv4-Listeners.
	 * 
	 * @param ipv4 the IPv4 address
	 */
	public synchronized void setIpv4(String ipv4) {
		this.setIpv4(ipv4, true);
	}

	/**
	 * Sets the IPv4 address and calls the SetIpv4-Listeners.
	 * 
	 * @param ipv4          the IPv4 address
	 * @param callListeners whether to call the SetIpv4-Listeners
	 */
	public synchronized void setIpv4(String ipv4, boolean callListeners) {
		if (!Objects.equal(this.ipv4, ipv4)) { // on change
			if (callListeners) {
				this.onSetIpv4.forEach(listener -> listener.accept(ipv4));
			}
			this.ipv4 = ipv4;
		}
	}

	/*
	 * _sum/State
	 */
	private final List<BiConsumer<Level, Map<ChannelAddress, EdgeConfig.Component.Channel>>> onSetSumState = new CopyOnWriteArrayList<>();

	public void onSetSumState(BiConsumer<Level, Map<ChannelAddress, EdgeConfig.Component.Channel>> listener) {
		this.onSetSumState.add(listener);
	}

	private Set<ChannelAddress> lastActiveStateChannelsKeys = new HashSet<>();

	public synchronized void setSumState(Level sumState,
			Map<ChannelAddress, EdgeConfig.Component.Channel> activeStateChannels) {
		if (!Objects.equal(this.sumState, sumState) // on change
				|| !this.lastActiveStateChannelsKeys.equals(activeStateChannels.keySet())) { // on change
			this.lastActiveStateChannelsKeys = activeStateChannels.keySet();
			this.onSetSumState.forEach(listener -> listener.accept(sumState, activeStateChannels));
			this.sumState = sumState;
		}
	}

}
