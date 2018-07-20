package io.openems.backend.metadata.energydepot;

import io.openems.backend.metadata.api.User;
import io.openems.common.session.Role;

public class MyUser extends User {

	private  int edgeid;
	private String role;

	public MyUser(int id, String name, int edgeid, String role) {
		super(id, name);
		this.edgeid = edgeid;
		this.role = role;
		// TODO Auto-generated constructor stub
	}

	public int getEdgeid() {
		return this.edgeid;
	}
	
	public String getRole() {
		return this.role;
	}

	

}
