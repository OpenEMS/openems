package io.openems.common.accesscontrol;

import java.util.Objects;

/**
 * This class represents a machine or another application which wants to communicate with 'us'. Since a application cannot login with a
 * username and password (see {@link User}), it is holding an application key which gets used for the authentication
 *
 * @author Sebastian.Walbrun
 */
public class Machine {

	private String id;
	private String name;
	private String description;
	private String apiKey;
	private ApplicationType type;
	private RoleId role;

	String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	ApplicationType getType() {
		return type;
	}

	public void setType(ApplicationType type) {
		this.type = type;
	}

	RoleId getRole() {
		return role;
	}

	public void setRole(RoleId role) {
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Machine machine = (Machine) o;
		return Objects.equals(id, machine.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
