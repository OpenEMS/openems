package io.openems.edge.bridge.can.api;

import java.util.Arrays;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.data.CanSimulationData;

public class BasicCanSimulation implements CanSimulationData {

	protected static final Logger log = LoggerFactory.getLogger(BasicCanSimulation.class);

	public static class CanSimuResponseData implements CanRxTxData {
		public static final int DEFAULT_CYCLE_TIME_SCHEDULER_IN_MS = 200;

		private int adr;
		private final boolean extAdr;
		private final byte[] data;
		private final int defaultCycleTime;
		private final int defaultResendCntr;
		private int workerResendCntr;
		private int workerCycleTime;

		private int simulationStateCntr = 0;

		public CanSimuResponseData(int canAddress, byte[] canData) {
			this(canAddress, false, 0, 0, canData);
		}

		public CanSimuResponseData(int canAddress, boolean extendedCanFrame, int cycleTime, int resendCntr,
				byte[] canData) {
			this.extAdr = extendedCanFrame;
			this.adr = canAddress;
			this.data = canData;
			this.defaultCycleTime = cycleTime;
			this.workerCycleTime = cycleTime;
			this.defaultResendCntr = resendCntr;
			this.workerResendCntr = resendCntr;
		}

		@Override
		public int getAddress() {
			return this.adr;
		}

		@Override
		public void setAddress(int addr) {
			this.adr = addr;
		}

		@Override
		public boolean isExtendedAddress() {
			return this.extAdr;
		}

		@Override
		public int getLength() {
			return this.data == null ? 0 : this.data.length;
		}

		@Override
		public byte[] getData() {
			return this.data;
		}

		private boolean checkSendNow() {
			if (this.workerResendCntr > 0) {
				this.workerCycleTime -= DEFAULT_CYCLE_TIME_SCHEDULER_IN_MS;
				if (this.workerCycleTime <= 0) {
					this.workerCycleTime = this.defaultCycleTime;
					this.workerResendCntr--;
					return true;
				}
			}
			return false;
		}

		private boolean allResend() {
			return this.workerResendCntr == 0;
		}

		private void resetSimulation() {
			this.workerResendCntr = this.defaultResendCntr;
			this.workerCycleTime = this.defaultCycleTime;
		}

		public int getSimulationStateCntr() {
			return this.simulationStateCntr;
		}

		public void setSimulationStateCntr(int newSimStateCntr) {
			this.simulationStateCntr = newSimStateCntr;
		}

		/**
		 * Is called before the response data is put to the receive queue.
		 */
		public void onBeforePutToCanReceiveQueue() {

		}
	}

	public static class CanSimuRequestResponseData {

		public static final byte MASK_IGNORE = (byte) 0x0;
		public static final byte MASK_EVALUATE = (byte) 0xff;

		protected int address;
		protected int length;
		protected byte[] filterMask;
		protected byte[] filterValues;
		protected CanSimuResponseData[] responses;

		public CanSimuRequestResponseData() {
		}

		public CanSimuRequestResponseData(int address, int length, byte[] filterMask, byte[] filterValues,
				CanSimuResponseData[] responses) {
			this();
			this.address = address;
			this.length = length;
			this.filterMask = filterMask;
			this.filterValues = filterValues;
			this.responses = responses;
		}

		public int getReqAddress() {
			return this.address;
		}

		public int getReqLength() {
			return this.length;
		}

		public byte[] getReqFilterMask() {
			return this.filterMask;
		}

		public byte[] getReqFilterValues() {
			return this.filterValues;
		}

		public CanSimuResponseData[] getResponses() {
			return this.responses;
		}

		@Override
		public String toString() {
			return "SimuReqResp [" + String.format("%7d/X%06x %d", this.address, this.address, this.length) + ", "
					+ CanUtils.getHexInfo(this.filterValues) + " (filterValues) " + "]";
		}

		private CanSimuResponseData[] getResponsesByRequest(CanRxTxData reqFrame) {
			return this.getResponses();
		}
	}

	private final Vector<CanSimuResponseData> requestedFrames = new Vector<>();
	private final Vector<CanSimuResponseData[]> readSimulation = new Vector<>();
	private final Vector<CanSimuRequestResponseData> onWriteSimulation = new Vector<>();

	private static final int DUMMY_ADDRESS = 0xffff1234;
	private static final CanSimuResponseData EMPTYDUMMY = new CanSimuResponseData(DUMMY_ADDRESS, null) {
		@Override
		public void onBeforePutToCanReceiveQueue() {
		}
	};

	/**
	 * All frames added with this method, will by "received" by the OpenEMS CAN
	 * driver, beginning with idx 0.
	 *
	 * @param readData the simulated read data
	 */
	public void addToCanReadSimulation(CanSimuResponseData[] readData) {
		this.readSimulation.add(readData);
	}

	/**
	 * If the filtered frame (idx0) will be received, all other frames (idx 1..)
	 * will be send.
	 *
	 * @param reqRespData idx 0 is the one which will be used as filter
	 */
	public void addToWriteSimulation(CanSimuRequestResponseData reqRespData) {
		this.onWriteSimulation.add(reqRespData);
	}

	private void simulateFrame(Vector<CanRxTxData> v, CanSimuResponseData[] simCan) {
		var cntr = simCan[0].getSimulationStateCntr();
		if (!simCan[cntr].allResend()) {
			if (simCan[cntr].checkSendNow()) {
				v.add(simCan[cntr]);
				simCan[cntr].onBeforePutToCanReceiveQueue();
			}
		} else {
			cntr++;
			if (cntr >= simCan.length) {
				cntr = 0;
			}
			simCan[cntr].resetSimulation();
		}
		simCan[0].setSimulationStateCntr(cntr);
	}

	private void appendRequestedFrames(Vector<CanRxTxData> v) {
		while (true) {
			if (this.requestedFrames.isEmpty()) {
				return;
			}
			if (this.requestedFrames.get(0).getAddress() == DUMMY_ADDRESS) {
				this.requestedFrames.remove(0);
				return; // we do not add any frames in this read Loop
			}
			var d = this.requestedFrames.remove(0);
			v.add(d);
		}
	}

	@Override
	public CanRxTxData[] receiveData() {
		var v = new Vector<CanRxTxData>();

		for (CanSimuResponseData[] currentCanSet : this.readSimulation) {
			this.simulateFrame(v, currentCanSet);
		}
		this.appendRequestedFrames(v);
		// done
		if (v.size() > 0) {
			return Arrays.copyOf(v.toArray(), v.size(), CanRxTxData[].class);
		}
		return null;
	}

	private boolean checkFrame(CanRxTxData transmittedFrame, CanSimuRequestResponseData reqRespData) {
		if (transmittedFrame.getAddress() != reqRespData.getReqAddress() || transmittedFrame.getData() == null
				|| transmittedFrame.getLength() != reqRespData.getReqLength()) {
			return false;
		}
		for (var i = 0; i < reqRespData.getReqLength(); i++) {
			if (reqRespData.getReqFilterMask()[i] == CanSimuRequestResponseData.MASK_IGNORE) {
				continue;
			}
			if (reqRespData.getReqFilterValues()[i] != transmittedFrame.getData()[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void sendData(CanRxTxData transmitFrame) {
		for (CanSimuRequestResponseData reqRespData : this.onWriteSimulation) {
			if (this.checkFrame(transmitFrame, reqRespData)) {

				var responses = reqRespData.getResponsesByRequest(transmitFrame);
				if (responses != null) {
					// send all responses for this frame at once
					for (var i = 0; i < responses.length; i++) {
						this.requestedFrames.add(responses[i]);
						if (i > 1) { // slow down responses
							this.requestedFrames.add(EMPTYDUMMY);
						}
					}
				}
			}
		}
	}

}
