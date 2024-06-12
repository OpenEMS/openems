package io.openems.edge.bridge.can;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.bridge.can.api.data.CanRxTxData;

public class CanReadWorker extends AbstractWorker {

	private static final int WORKER_CYCLE_TIME = 100;

	/**
	 * using a map like a special queue with constraint that only one frame per CAN
	 * ID is stored.
	 */
	private final ConcurrentHashMap<Integer, CanRxTxData> frameQueue;
	private final AbstractCanBridge parent;
	private final AtomicInteger countAll = new AtomicInteger(0);
	private final AtomicInteger countReadTaskCycle = new AtomicInteger(0);
	private final Vector<CanRxTxData> v = new Vector<>();
	private final AtomicBoolean isBlocked = new AtomicBoolean(false);

	public CanReadWorker(AbstractCanBridge parent) {
		this.parent = parent;
		this.frameQueue = new ConcurrentHashMap<>();
	}

	@Override
	public void activate(String name) {
		this.isBlocked.set(false);
		super.activate(name);
		this.frameQueue.clear();
		this.countReadTaskCycle.set(0);
		this.countAll.set(0);
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected final int getCycleTime() {
		return WORKER_CYCLE_TIME;
	}

	@Override
	protected void forever() throws InterruptedException {
		this.countReadTaskCycle.getAndIncrement();
		try {
			var canConn = this.parent.getCanConnection();
			if (!canConn.isOpen() || this.isBlocked.get()) {
				return;
			}
			CanRxTxData[] rxFrames = canConn.receiveAll();
			if (rxFrames != null && rxFrames.length > 0) {
				this.countAll.getAndAdd(rxFrames.length);
				for (CanRxTxData rxFrame : rxFrames) {
					this.frameQueue.put(Integer.valueOf(rxFrame.getAddress()), rxFrame);
				}
			}

		} catch (OpenemsException e) {
			throw new InterruptedException(e.getMessage());
		}
	}

	protected Integer getFramesCountAll() {
		return this.countAll.getAndSet(0);
	}

	protected Integer getCountReadTaskCyclces() {
		return this.countReadTaskCycle.getAndSet(0);
	}

	/**
	 * NOTE: this method is called by another Worker.
	 *
	 * @return The queued frames as {@link CanRxTxData}-array.
	 */
	protected CanRxTxData[] fetchQueuedFrames() {
		this.v.clear();
		{
			this.isBlocked.set(true); // NOTE: still a chance that the frameQueue is filled while we have blocked it
			var c = this.frameQueue.values();
			var itr = c.iterator();
			// copy values and clear map
			while (itr.hasNext()) {
				this.v.add(itr.next());
				itr.remove();
			}
			this.isBlocked.set(false);
		}
		return this.v.toArray(new CanRxTxData[this.v.size()]);
	}

}
