package io.openems.backend.common.metadata;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gson.JsonObject;

import io.openems.backend.common.event.BackendEventConstants;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class Edge {

	private final Logger log = LoggerFactory.getLogger(Edge.class);
	private final Metadata parent;

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

	private final List<EdgeUser> user;

	public Edge(Metadata parent, String id, String comment, State state, String version, String producttype,
			Level sumState, EdgeConfig config, ZonedDateTime lastMessage, ZonedDateTime lastUpdate) {
		this.id = id;
		this.comment = comment;
		this.state = state;
		this.version = SemanticVersion.fromStringOrZero(version);
		this.producttype = producttype;
		this.sumState = sumState;
		this.config = config;
		this.lastMessage = lastMessage;
		this.lastUpdate = lastUpdate;

		this.parent = parent;
		this.user = new ArrayList<>();
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

	public boolean isOnline() {
		return this.isOnline;
	}

	/**
	 * Marks this Edge as being online. This is called by an event listener.
	 *
	 * @param isOnline true if the Edge is online
	 */
	public synchronized void setOnline(boolean isOnline) {
		if (this.isOnline != isOnline) {
			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_ONLINE) //
					.addArg(Events.OnSetOnline.EDGE, this) //
					.addArg(Events.OnSetOnline.IS_ONLINE, isOnline) //
					.send(); //

			this.isOnline = isOnline;
		}
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
		EdgeConfigDiff diff = EdgeConfigDiff.diff(config, this.getConfig());
		if (diff.isDifferent()) {
			if (callListeners) {
				EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_CONFIG) //
						.addArg(Events.OnSetConfig.EDGE, this) //
						.addArg(Events.OnSetConfig.CONFIG, config) //
						.addArg(Events.OnSetConfig.DIFF, diff) //
						.send(); //
			}
			this.config = config;
		}
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
			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_LAST_MESSAGE_TIMESTAMP) //
					.addArg(Events.OnSetLastMessageTimestamp.EDGE, this) //
					.send(); //
		}
		var now = ZonedDateTime.now(ZoneOffset.UTC);
		this.lastMessage = now;
	}

	/**
	 * Returns the Last-Message-Timestamp.
	 *
	 * @return Last-Message-Timestamp in UTC Timezone
	 */
	public ZonedDateTime getLastMessageTimestamp() {
		return this.lastMessage;
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
			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_LAST_UPDATE_TIMESTAMP) //
					.addArg(Events.OnSetLastUpdateTimestamp.EDGE, this) //
					.send(); //
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

				EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_VERSION) //
						.addArg(Events.OnSetVersion.EDGE, this) //
						.addArg(Events.OnSetVersion.VERSION, version) //
						.send(); //
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

				EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_PRODUCTTYPE) //
						.addArg(Events.OnSetProducttype.EDGE, this) //
						.addArg(Events.OnSetProducttype.PRODUCTTYPE, producttype) //
						.send(); //
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
				EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_SUM_STATE)
						.addArg(Events.OnSetSumState.EDGE, this) //
						.addArg(Events.OnSetSumState.SUM_STATE, sumState) //
						.send();
			}
			this.sumState = sumState;
		}
	}

	/**
	 * Add User to UserList.
	 *
	 * @param user to add
	 */
	public synchronized void addUser(EdgeUser user) {
		this.user.add(user);
	}

	/**
	 * Get list of users.
	 *
	 * @return user as List of EdgeUser
	 */
	public List<EdgeUser> getUser() {
		return this.user;
	}

	/**
	 * Defines all Events an Edge can throw.
	 */
	public static final class Events {
		private static final String TOPIC_BASE = BackendEventConstants.TOPIC_BASE + "edge/";
		public static final String ALL_EVENTS = Events.TOPIC_BASE + "*";

		public static final String ON_SET_ONLINE = Events.TOPIC_BASE + "TOPIC_ON_SET_ONLINE";

		public static final class OnSetOnline {
			public static final String EDGE = "Edge:Edge";
			public static final String IS_ONLINE = "IsOnline:boolean";
		}

		public static final String ON_SET_VERSION = Events.TOPIC_BASE + "TOPIC_ON_SET_VERSION";

		public static final class OnSetVersion {
			public static final String EDGE = "Edge:Edge";
			public static final String VERSION = "Version:SemanticVersion";
		}

		public static final String ON_SET_PRODUCTTYPE = Events.TOPIC_BASE + "TOPIC_ON_SET_PRODUCTTYPE";

		public static final class OnSetProducttype {
			public static final String EDGE = "Edge:Edge";
			public static final String PRODUCTTYPE = "Producttype:String";
		}

		public static final String ON_SET_SUM_STATE = Events.TOPIC_BASE + "TOPIC_ON_SET_SUM_STATE";

		public static final class OnSetSumState {
			public static final String EDGE = "Edge:Edge";
			public static final String SUM_STATE = "SumState:Level";
		}

		public static final String ON_SET_CONFIG = Events.TOPIC_BASE + "TOPIC_ON_SET_CONFIG";

		public static final class OnSetConfig {
			public static final String EDGE = "Edge:Edge";
			public static final String CONFIG = "Config:EdgeConfig";
			public static final String DIFF = "Diff:EdgeConfigDiff";
		}

		public static final String ON_SET_LAST_MESSAGE_TIMESTAMP = Events.TOPIC_BASE
				+ "TOPIC_ON_SET_LAST_MESSAGE_TIMESTAMP";

		public static final class OnSetLastMessageTimestamp {
			public static final String EDGE = "Edge:Edge";
		}

		public static final String ON_SET_LAST_UPDATE_TIMESTAMP = Events.TOPIC_BASE + "ON_SET_LAST_UPDATE_TIMESTAMP";

		public static final class OnSetLastUpdateTimestamp {
			public static final String EDGE = "Edge:Edge";
		}
	}
}
