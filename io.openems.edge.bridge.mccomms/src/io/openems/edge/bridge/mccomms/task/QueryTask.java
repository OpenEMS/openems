package io.openems.edge.bridge.mccomms.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.MCCommsBridge;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QueryTask {
	
	private WriteTask writeTask;
	private long replyTimeOut;
	private TimeUnit replyTimeOutUnit;
	private ListenTask[] listenTasks;
	private ScheduledFuture future;
	private Runnable callback;
	
	private QueryTask(WriteTask writeTask, long replyTimeOut, TimeUnit replyTimeOutUnit, ListenTask ... listenTasks) {
		this.writeTask = writeTask;
		this.replyTimeOut = replyTimeOut;
		this.replyTimeOutUnit = replyTimeOutUnit;
		this.listenTasks = listenTasks;
	}
	
	public static QueryTask newCommandOnlyQuery(int thisAddress, int otherAddress, int command, int replyTimeOut, TimeUnit replyTimeOutUnit, ListenTask...replyListenTasks) throws OpenemsException {
		return new QueryTask(
				WriteTask.newCommandOnlyWriteTask(thisAddress, otherAddress, command),
				replyTimeOut,
				replyTimeOutUnit,
				replyListenTasks
		);
	}
	
	public void doReceiveCallback(MCCommsBridge bridge, Runnable callback) {
		new Thread(() -> {
			long end = System.nanoTime() + replyTimeOutUnit.toNanos(replyTimeOut);
			for (ListenTask listenTask: listenTasks) {
				try {
					listenTask.get(end - System.nanoTime(), TimeUnit.NANOSECONDS).updateElementChannels();
				} catch (InterruptedException | ExecutionException | TimeoutException | OpenemsException e) {
					//TODO handle exceptions
				}
			}
			for (ListenTask listenTask : listenTasks) {
				bridge.removeListenTask(listenTask);
			}
			callback.run();
		}).start();
	}
	
	public void addAllListenTasks(MCCommsBridge bridge) {
		for (ListenTask listenTask : listenTasks) {
			bridge.addListenTask(listenTask);
		}
	}
	
	public void queryOnce(MCCommsBridge bridge) {
		bridge.addQueryTask(this);
	}
	
	public void queryRepeatedly(MCCommsBridge bridge, long timePeriod, TimeUnit timeUnit) {
		future = bridge.getScheduledExecutorService().scheduleAtFixedRate(() -> bridge.addQueryTask(this), 0, timePeriod, timeUnit);
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
	
	public byte[] getBytes() {
		return writeTask.getBytes();
	}
}
