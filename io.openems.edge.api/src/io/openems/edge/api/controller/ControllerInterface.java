package io.openems.edge.api.controller;

import java.util.Set;

import io.openems.edge.api.message.Message;

public interface ControllerInterface {

	ControllerState getStatus();
	
	void executeLogic();
	
	String getId();
	
	Set<Message> getControllerMessages();
}
