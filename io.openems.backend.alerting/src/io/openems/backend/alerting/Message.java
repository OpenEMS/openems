package io.openems.backend.alerting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import io.openems.backend.common.metadata.EdgeUser;

/**
 * Properties for one notification.
 */
public class Message implements Comparable<Message> {
	private final String edgeId;
	private final List<EdgeUser> userList;
	private final ZonedDateTime notifyStamp;

	public Message(ZonedDateTime notifyStamp, List<EdgeUser> users, String edgeId) {
		this.edgeId = edgeId;
		this.userList = users;
		this.notifyStamp = notifyStamp;
	}

	public ZonedDateTime getTimeStamp() {
		return this.notifyStamp.withZoneSameInstant(ZoneId.systemDefault());
	}

	@Override
	public int compareTo(Message o) {
		return this.notifyStamp.compareTo(o.notifyStamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.notifyStamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Message other = (Message) obj;
		return Objects.equals(this.notifyStamp, other.notifyStamp);
	}

	/**
	 * Add User to UserList.
	 *
	 * @param user to add
	 */
	public void addUser(EdgeUser user) {
		this.userList.add(user);
	}

	public List<EdgeUser> getUser() {
		return this.userList;
	}

	public ZonedDateTime getNotifyStamp() {
		return this.notifyStamp;
	}

	public String getEdgeId() {
		return this.edgeId;
	}
}
