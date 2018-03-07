package io.openems.edge.api.controller;

public interface ControllerInterface {

	ControllerStatus getStatus();
	
	void executeLogic();
	
	String getId();
}
