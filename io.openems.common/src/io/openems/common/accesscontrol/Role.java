package io.openems.common.accesscontrol;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * Model object that represents a security role.
 */
public class Role {

	private RoleId id;

	private String description;

	private Set<Role> parents = new HashSet<>();

	// <edgeId, <Json-Rpc Identifier, permission>>
	private Map<String, Map<String, ExecutePermission>> jsonRpcPermissions = new HashMap<>();

	// <edgeId, <channelAddress, permission>>
	private Map<String, Map<ChannelAddress, AccessMode>> channelPermissions = new HashMap<>();

	public Role(RoleId id) {
		this.id = id;
	}

	public Role() {
	}

	public Role(Set<Role> parents) {
		this.parents = parents;
	}

	void setParents(Set<Role> parents) {
		this.parents = parents;
	}

	public void addParent(Role parent) {
		this.parents.add(parent);
	}

	Set<Role> getParents() {
		return parents;
	}

	public RoleId getRoleId() {
		return id;
	}

	public void setId(RoleId id) {
		this.id = id;
	}

	String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addJsonRpcPermission(String edge, Map<String, ExecutePermission> permissions) {
		if (this.jsonRpcPermissions.get(edge) == null) {
			this.jsonRpcPermissions.put(edge, permissions);
		} else {
			this.jsonRpcPermissions.get(edge).putAll(permissions);
		}
	}

	Optional<Map<String, ExecutePermission>> getJsonRpcPermissions(String edgeId) {
		return this.jsonRpcPermissions.entrySet().stream()
			.filter(entry -> entry.getKey().equals(edgeId))
			.map(Map.Entry::getValue).findFirst();
	}

	Map<String, Map<String, ExecutePermission>> getJsonRpcPermissions() {
		return jsonRpcPermissions;
	}

	/**
	 * Gets all edgeIds on which the this role has at least access on one channel or one execution.
	 *
	 * @return the edges
	 */
	Set<String> getEdgeIds() {
		Set<String> retVal = new HashSet<>();
		Stack<Role> parentsToGo = new Stack<>();
		parentsToGo.push(this);

		while (!parentsToGo.isEmpty()) {
			Role parent = parentsToGo.pop();

			// add the parents of the parents
			parent.parents.forEach(parentsToGo::push);
			retVal.addAll(parent.jsonRpcPermissions.keySet());
			retVal.addAll(parent.channelPermissions.keySet());
		}

		return retVal;
	}

	Map<String, ExecutePermission> getJsonRpcPermissionsWithInheritance(String edgeId) {
		Map<String, ExecutePermission> retVal = new HashMap<>();
		Stack<Role> parentsToGo = new Stack<>();
		parentsToGo.push(this);

		while (!parentsToGo.isEmpty()) {
			Role parent = parentsToGo.pop();

			// add the parents of the parents
			parent.parents.forEach(parentsToGo::push);
			parent.jsonRpcPermissions.entrySet().stream()
				.filter(entry -> entry.getKey().equals(edgeId))
				.map(Map.Entry::getValue).findFirst().ifPresent(retVal::putAll);
		}

		return retVal;
	}

	Map<String, Map<ChannelAddress, AccessMode>> getChannelPermissions() {
		return channelPermissions;
	}

	Optional<Map<ChannelAddress, AccessMode>> getChannelPermissions(String edgeId) {
		return this.channelPermissions.entrySet().stream()
			.filter(entry -> entry.getKey().equals(edgeId))
			.map(Map.Entry::getValue).findFirst();
	}

	Map<ChannelAddress, AccessMode> getChannelPermissionsWithInheritance(String edgeId) {

		Map<ChannelAddress, AccessMode> retVal = new HashMap<>();
		Stack<Role> parentsToGo = new Stack<>();
		parentsToGo.push(this);

		while (!parentsToGo.isEmpty()) {
			Role parent = parentsToGo.pop();

			// add the parents of the parents
			parent.parents.forEach(parentsToGo::push);
			parent.channelPermissions.entrySet().stream()
				.filter(entry -> entry.getKey().equals(edgeId))
				.map(Map.Entry::getValue).findFirst().ifPresent(retVal::putAll);
		}

		return retVal;
	}

	public void addChannelPermissions(String edge, Map<ChannelAddress, AccessMode> permissions) {
		if (this.channelPermissions.get(edge) == null) {
			this.channelPermissions.put(edge, permissions);
		} else {
			this.channelPermissions.get(edge).putAll(permissions);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Role role = (Role) o;
		return Objects.equals(id, role.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
