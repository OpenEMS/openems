package io.openems.backend.metadata.api;

public class User {
	private final int id;
	private String name;
	
	public User(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
}
