package io.openems.backend.metadata.energydepot;

import io.openems.backend.metadata.api.User;

public class MyUser extends User {

	private  int edgeid;

	public MyUser(int id, String name, int edgeid) {
		super(id, name);
		this.edgeid = edgeid;
		// TODO Auto-generated constructor stub
	}

	public int getEdgeid() {
		return this.edgeid;
	}

	

}
