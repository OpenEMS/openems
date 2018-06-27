package io.openems.edge.ess.kostal.piko;

public class ReadTasksManager {
	
	private final EssKostalPiko parent;
	private final ReadTask[] tasks;
	
	public ReadTasksManager(EssKostalPiko parent, ReadTask... tasks) {
		this.parent = parent;
		this.tasks = tasks;
	}
	
}
