package io.openems.edge.bridge.mccomms.task;

import io.openems.edge.bridge.mccomms.MCCommsBridge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QueryTask {
	
	private WriteTask writeTask;
	private HashSet<ListenTask> listenTasks;
	private ScheduledFuture future;
	
	public QueryTask(WriteTask writeTask, ListenTask ... listenTasks) {
		this.writeTask = writeTask;
		this.listenTasks = new HashSet<>();
		this.listenTasks.addAll(Arrays.asList(listenTasks));
	}
	
	public void queryOnce(MCCommsBridge bridge) {
		for (ListenTask listenTask : listenTasks) {
			bridge.addListenTask(listenTask);
		}
		writeTask.sendOnce(bridge);
		for (ListenTask listenTask : listenTasks) {
			bridge.removeListenTask(listenTask);
		}
	}
	
	public void queryRepeatedly(MCCommsBridge bridge, long timePeriod, TimeUnit timeUnit) {
		for (ListenTask listenTask : listenTasks) {
			bridge.addListenTask(listenTask);
		}
		future = writeTask.sendRepeatedly(bridge, timePeriod, timeUnit);
	}
	
	public void cancel(MCCommsBridge bridge) {
		if (Optional.ofNullable(future).isPresent()) {
			future.cancel(false);
			for (ListenTask listenTask : listenTasks) {
				bridge.removeListenTask(listenTask);
			}
			future = null;
		}
	}
}
