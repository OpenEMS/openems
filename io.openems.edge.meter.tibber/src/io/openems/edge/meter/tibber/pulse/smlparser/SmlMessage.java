package io.openems.edge.meter.tibber.pulse.smlparser;

public class SmlMessage extends AbstractSmlObject {

	protected byte[] transactionId;
	protected Integer groupNo;
	protected Integer abortOnError;
	protected SmlMessageBody messageBody;
	protected Integer crc16Actual;
	protected Integer crc16Expected;
	protected Boolean crc16Ok;

	public byte[] getTransactionId() {
		return this.transactionId;
	}

	public Integer getGroupNo() {
		return this.groupNo;
	}

	public Integer getAbortOnError() {
		return this.abortOnError;
	}

	public SmlMessageBody getMessageBody() {
		return this.messageBody;
	}

	public Integer getCrc16Actual() {
		return this.crc16Actual;
	}

	public Integer getCrc16Expected() {
		return this.crc16Expected;
	}

	public Boolean getCrc16Ok() {
		return this.crc16Ok;
	}

}
