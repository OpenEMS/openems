package io.openems.edge.bridge.mccomms;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.bridge.mccomms.task.WriteTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public interface IMCCommsBridge extends OpenemsComponent {
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		
		private final Doc doc;
		
		private ChannelId(Doc doc) {
			this.doc = doc;
		}
		
		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	void addListenTask(ListenTask listenTask);
	
	void removeListenTask(ListenTask listenTask);
	
	void addWriteTask(WriteTask writeTask);
	
	void addQueryTask(QueryTask queryTask);
	
	ScheduledExecutorService getScheduledExecutorService();
	
	ExecutorService getSingleThreadExecutor();
	
	void logError(Throwable cause);
	
}
