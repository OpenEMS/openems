package io.openems.backend.metadata.wordpress;

import java.util.ArrayList;

import io.openems.backend.metadata.api.BackendUser;

public class MyUser extends BackendUser {

	private ArrayList<String> edgeids;
	private String role;

	public MyUser(String id, String name, ArrayList<String> edgeids, String role) {
		super(id, name);
		this.edgeids = edgeids;
		this.role = role;
	}

	public ArrayList<String> getEdgeids() {
		return this.edgeids;
	}

	public String getRole() {
		return this.role;
	}

}
