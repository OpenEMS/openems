package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gson.JsonObject;

import io.openems.backend.common.event.BackendEventConstants;
import io.openems.common.event.EventBuilder;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class Edge {

	private final Logger log = LoggerFactory.getLogger(Edge.class);
	private final Metadata parent;

	private final String id;
	private String comment;
	private final AtomicReference<SemanticVersion> version = new AtomicReference<>(SemanticVersion.ZERO);
	private final AtomicReference<String> producttype = new AtomicReference<>("");
	private final AtomicReference<ZonedDateTime> lastmessage = new AtomicReference<>(null);
	private boolean isOnline = false;

	private final List<EdgeUser> user;

	public Edge(Metadata parent, String id, String comment, String version, String producttype,
			ZonedDateTime lastmessage) {
		this.id = id;
		this.comment = comment;

		this.parent = parent;
		this.user = new ArrayList<>();

		// Avoid initial events
		this.setProducttype(producttype, false);
		this.setVersion(SemanticVersion.fromStringOrZero(version), false);
		this.setLastmessage(lastmessage, false);
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

	/**
	 * Gets this {@link Edge} as {@link JsonObject}.
	 *
	 * @return a {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("comment", this.comment) //
				.addProperty("version", this.version.get().toString()) //
				.addProperty("producttype", this.producttype.get()) //
				.addProperty("online", this.isOnline) //
				.addPropertyIfNotNull("lastmessage", this.lastmessage.get()) //
				.build();
	}

	@Override
	public String toString() {
		return "Edge [" //
				+ "id=" + this.id + ", " //
				+ "comment=" + this.comment + ", " //
				+ "version=" + this.version + ", " //
				+ "producttype=" + this.producttype + ", " //
				+ "lastmessage=" + this.lastmessage + ", " //
				+ "isOnline=" + this.isOnline //
				+ "]";
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	public boolean isOffline() {
		return !this.isOnline;
	}

	/**
	 * Marks this Edge as being online. This is called by an event listener.
	 *
	 * @param isOnline true if the Edge is online
	 */
	public synchronized void setOnline(boolean isOnline) {
		if (this.isOnline != isOnline) {
			this.isOnline = isOnline;

			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_ONLINE) //
					.addArg(Events.OnSetOnline.EDGE_ID, this.getId()) //
					.addArg(Events.OnSetOnline.IS_ONLINE, isOnline) //
					.send(); //
		}
	}

	/**
	 * Sets the Last-Message-Timestamp to now() (truncated to Minutes) and emits a
	 * ON_SET_LASTMESSAGE event; but only max one event per Minute.
	 */
	public void setLastmessage() {
		this.setLastmessage(ZonedDateTime.now());
	}

	/**
	 * Sets the Last-Message-Timestamp (truncated to Minutes) and emits a
	 * ON_SET_LASTMESSAGE event; but only max one event per Minute.
	 * 
	 * @param timestamp the Last-Message-Timestamp
	 */
	public void setLastmessage(ZonedDateTime timestamp) {
		this.setLastmessage(timestamp, true);
	}

	private void setLastmessage(ZonedDateTime timestamp, boolean emitEvent) {
		if (timestamp == null) {
			return;
		}
		timestamp = timestamp.truncatedTo(ChronoUnit.MINUTES);
		var previousTimestamp = this.lastmessage.getAndSet(timestamp);
		if (emitEvent && (previousTimestamp == null || previousTimestamp.isBefore(timestamp))) {
			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_LASTMESSAGE) //
					.addArg(Events.OnSetLastmessage.EDGE, this) //
					.send();
		}
	}

	/**
	 * Returns the Last-Message-Timestamp.
	 *
	 * @return Last-Message-Timestamp in UTC Timezone
	 */
	public ZonedDateTime getLastmessage() {
		return this.lastmessage.get();
	}

	/*
	 * Version
	 */
	public SemanticVersion getVersion() {
		return this.version.get();
	}

	/**
	 * Sets the version and emits a ON_SET_VERSION event.
	 *
	 * @param version the version
	 */
	public void setVersion(SemanticVersion version) {
		this.setVersion(version, true);
	}

	private void setVersion(SemanticVersion version, boolean emitEvent) {
		if (version == null) {
			version = SemanticVersion.ZERO;
		}
		var oldVersion = this.version.getAndSet(version);
		if (emitEvent && !Objects.equal(oldVersion, version)) { // on change
			this.log.info("Edge [" + this.getId() + "]: Update version from [" + oldVersion + "] to [" + version + "]");

			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_VERSION) //
					.addArg(Events.OnSetVersion.EDGE, this) //
					.addArg(Events.OnSetVersion.VERSION, version) //
					.send();
		}
	}

	/*
	 * Producttype
	 */
	public String getProducttype() {
		return this.producttype.get();
	}

	/**
	 * Sets the Producttype and emits a ON_SET_PRODUCTTYPE event.
	 *
	 * @param producttype the Producttype
	 */
	public void setProducttype(String producttype) {
		this.setProducttype(producttype, true);
	}

	private void setProducttype(String producttype, boolean emitEvent) {
		if (producttype == null) {
			producttype = "";
		}
		var oldProducttype = this.producttype.getAndSet(producttype);
		if (emitEvent && !Objects.equal(oldProducttype, producttype)) { // on change
			this.log.info("Edge [" + this.getId() + "]: Update Product-Type from [" + oldProducttype + "] to ["
					+ producttype + "]");

			EventBuilder.from(this.parent.getEventAdmin(), Events.ON_SET_PRODUCTTYPE) //
					.addArg(Events.OnSetProducttype.EDGE, this) //
					.addArg(Events.OnSetProducttype.PRODUCTTYPE, producttype) //
					.send();
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

		private Events() {
		}

		private static final String TOPIC_BASE = BackendEventConstants.TOPIC_BASE + "edge/";
		public static final String ALL_EVENTS = Events.TOPIC_BASE + "*";

		public static final String ON_SET_ONLINE = Events.TOPIC_BASE + "ON_SET_ONLINE";

		public static final class OnSetOnline {
			public static final String EDGE_ID = "EdgeId:String";
			public static final String IS_ONLINE = "IsOnline:boolean";
		}

		public static final String ON_SET_VERSION = Events.TOPIC_BASE + "ON_SET_VERSION";

		public static final class OnSetVersion {
			public static final String EDGE = "Edge:Edge";
			public static final String VERSION = "Version:SemanticVersion";
		}

		public static final String ON_SET_PRODUCTTYPE = Events.TOPIC_BASE + "ON_SET_PRODUCTTYPE";

		public static final class OnSetProducttype {
			public static final String EDGE = "Edge:Edge";
			public static final String PRODUCTTYPE = "Producttype:String";
		}

		public static final String ON_SET_SUM_STATE = Events.TOPIC_BASE + "ON_SET_SUM_STATE";

		public static final class OnSetSumState {
			public static final String EDGE = "Edge:Edge";
			public static final String SUM_STATE = "SumState:Level";
		}

		public static final String ON_SET_CONFIG = Events.TOPIC_BASE + "ON_SET_CONFIG";

		public static final class OnSetConfig {
			public static final String EDGE = "Edge:Edge";
			public static final String CONFIG = "Config:EdgeConfig";
		}

		public static final String ON_SET_LASTMESSAGE = Events.TOPIC_BASE + "ON_SET_LASTMESSAGE";

		public static final class OnSetLastmessage {
			public static final String EDGE = "Edge:Edge";
		}

	}
}
