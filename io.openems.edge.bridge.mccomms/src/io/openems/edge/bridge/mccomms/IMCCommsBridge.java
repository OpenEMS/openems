package io.openems.edge.bridge.mccomms;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.bridge.mccomms.task.WriteTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface IMCCommsBridge extends OpenemsComponent {

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * Adds a listen task to the bridge. The bridge assigns incoming packets to
	 * listen tasks accordingly
	 * 
	 * @see ListenTask
	 * @param listenTask the listen task to add
	 */
	void addListenTask(ListenTask listenTask);

	/**
	 * Removes a listen task so the bridge will no longer assign incoming packets to
	 * it
	 * 
	 * @param listenTask the listen task to remove
	 */
	void removeListenTask(ListenTask listenTask);

	/**
	 * Adds a write task to a queue from which the bridge must acquire a buffers to
	 * write out. The write task will be dequeued once written and must be added
	 * again if it must be written more than once; see
	 * {@link WriteTask#sendRepeatedly(IMCCommsBridge, long, TimeUnit)}
	 * 
	 * @param writeTask the write task to be written
	 */
	void addWriteTask(WriteTask writeTask);

	/**
	 * Adds a query task to queue to be executed in order of queue addition. The
	 * query task will be dequeued once written and must be added again if it must
	 * be executed more than once; see
	 * {@link QueryTask#queryRepeatedly(long, TimeUnit)}
	 * 
	 * @param queryTask the query task to be executed
	 */
	void addQueryTask(QueryTask queryTask);

	/**
	 * @return a {@link ScheduledExecutorService} used to schedule tasks for
	 *         repetition
	 */
	ScheduledExecutorService getScheduledExecutorService();

	/**
	 * @return an {@link ExecutorService} used to do asynchronous blocking of the
	 *         bus during IO operations that require it
	 * @see QueryTask#doWriteWithReplyWriteLock(OutputStream, AtomicBoolean)
	 */
	ExecutorService getSingleThreadExecutor();

	/**
	 * Convenience method that exposes a logger for errors that occur while
	 * executing tasks
	 * 
	 * @see io.openems.edge.common.component.AbstractOpenemsComponent#logError(Logger,
	 *      String)
	 * @param cause the throwable carrying the message to be logged
	 */
	void logError(Throwable cause);

	/**
	 * Convenience method that exposes a logger for info emitted during task
	 * execution
	 * 
	 * @see io.openems.edge.common.component.AbstractOpenemsComponent#logInfo(Logger,
	 *      String)
	 * @param message the info to log
	 */
	void logInfo(String message);

}
