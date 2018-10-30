package io.openems.backend.metadata.energydepot;

import java.util.ArrayList;

import io.openems.backend.metadata.api.User;

public class MyUser extends User {

	private  ArrayList<Integer> edgeids;
	private String role;

	public MyUser(int id, String name, ArrayList<Integer> edgeids, String role) {
		super(id, name);
		this.edgeids = edgeids;
		this.role = role;
	}

	public ArrayList<Integer> getEdgeids() {
		return this.edgeids;
	}
	
	public String getRole() {
		return this.role;
	}

	

}
