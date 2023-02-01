package io.openems.edge.controller.api.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.DigitalIn;
import com.ghgande.j2mod.modbus.procimg.DigitalOut;
import com.ghgande.j2mod.modbus.procimg.FIFO;
import com.ghgande.j2mod.modbus.procimg.File;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalIn;
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalOut;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordUint16Reserved;

/**
 * This implementation answers Modbus-TCP Slave requests.
 */
public class MyProcessImage implements ProcessImage {

	private final Logger log = LoggerFactory.getLogger(MyProcessImage.class);

	protected final AbstractModbusTcpApi parent;

	protected MyProcessImage(AbstractModbusTcpApi parent) {
		this.parent = parent;
	}

	@Override
	public synchronized InputRegister[] getInputRegisterRange(int offset, int count) throws MyIllegalAddressException {
		try {
			this.parent.logDebug(this.log, "Reading Input Registers. Address [" + offset + "] Count [" + count + "].");
			var registers = this.getRegisterRange(offset, count);
			var result = new Register[registers.length];
			for (var i = 0; i < registers.length; i++) {
				result[i] = registers[i];
			}
			this.parent._setProcessImageFault(false);
			return result;

		} catch (Exception e) {
			this.parent._setProcessImageFault(true);
			e.printStackTrace();
			throw new MyIllegalAddressException(this, e.getMessage());
		}
	}

	@Override
	public synchronized Register[] getRegisterRange(int offset, int count) throws MyIllegalAddressException {
		this.parent.logDebug(this.log, "Reading Registers. Address [" + offset + "] Count [" + count + "].");

		try {
			/*
			 * Apply limitation from
			 * `com.ghgande.j2mod.modbus.msg.ModbusMessageImpl.setDataLength()` and handle
			 * exception properly
			 */
			var length = count * 2;
			if (length < 0 || length + 2 > 255) {
				throw new MyIllegalAddressException(this, "Invalid length: " + length + "; max. 126 registers allowed");
			}

			var records = this.parent.records.subMap(offset, offset + count);
			var result = new Register[count];
			for (var i = 0; i < count;) {
				// Get record for modbus address
				var ref = i + offset;
				var record = records.get(ref);
				if (record == null) {
					record = new ModbusRecordUint16Reserved(ref);
				}

				// Get Registers from Record
				var registers = this.getRecordValueRegisters(record);

				// make sure this Record fits
				if (result.length < i + registers.length) {
					throw new MyIllegalAddressException(this,
							"Record for Modbus address [" + ref + "] does not fit in Result.");
				}
				for (var j = 0; j < registers.length; j++) {
					result[i + j] = registers[j];
				}

				// increase i by word length
				i += registers.length;
			}
			this.parent._setProcessImageFault(false);
			return result;

		} catch (Exception e) {
			this.parent._setProcessImageFault(true);
			throw new MyIllegalAddressException(this, e.getMessage());
		}
	}

	@Override
	public synchronized Register getRegister(int ref) throws MyIllegalAddressException {
		this.parent.logDebug(this.log, "Get Register. Address [" + ref + "].");

		try {
			var record = this.parent.records.get(ref);

			// make sure the ModbusRecord is available
			if (record == null) {
				throw new MyIllegalAddressException(this, "Record for Modbus address [" + ref + "] is not available.");
			}

			// Get Registers from Record
			var registers = this.getRecordValueRegisters(record);

			// make sure this Record requires only one Register/Word
			if (registers.length > 1) {
				throw new MyIllegalAddressException(this,
						"Record for Modbus address [" + ref + "] requires more than one Register.");
			}

			this.parent._setProcessImageFault(false);
			return registers[0];

		} catch (Exception e) {
			this.parent._setProcessImageFault(true);
			throw new MyIllegalAddressException(this, e.getMessage());
		}
	}

	/**
	 * Get value as byte-array and convert it to InputRegisters.
	 *
	 * @param record the record
	 * @return the Register
	 */
	private Register[] getRecordValueRegisters(ModbusRecord record) {
		var result = new MyRegister[record.getType().getWords()];
		final OpenemsComponent component;
		{
			var cmp = this.parent.getPossiblyDisabledComponent(record.getComponentId());
			if (cmp != null && !cmp.isEnabled()) {
				this.parent.logWarn(this.log, "Trying to access disabled Component [" + cmp.id() + "] for " + record);
				component = null;
			} else {
				component = cmp;
			}
		}

		var value = record.getValue(component);
		for (var j = 0; j < value.length / 2; j++) {
			result[j] = new MyRegister(j, value[j * 2], value[j * 2 + 1], //
					/*
					 * On Set-Value event:
					 */
					register -> {
						record.writeValue(register.getIndex(), register.getByte1(), register.getByte2());
					});
		}
		return result;
	}

	/**********************************************
	 * From here, the methods are not implemented!.
	 **********************************************
	 */

	@Override
	public synchronized InputRegister getInputRegister(int ref) {
		this.parent.logWarn(this.log, "getInputRegister is not implemented");
		this.parent._setProcessImageFault(true);
		return new SimpleInputRegister(0);
	}

	@Override
	public synchronized int getInputRegisterCount() {
		this.parent.logWarn(this.log, "getInputRegisterCount is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

	@Override
	public synchronized DigitalOut[] getDigitalOutRange(int offset, int count) {
		this.parent.logWarn(this.log, "getDigitalOutRange is not implemented");
		this.parent._setProcessImageFault(true);
		var result = new DigitalOut[count];
		for (var i = 0; i < count; i++) {
			result[i] = new SimpleDigitalOut(false);
		}
		return result;
	}

	@Override
	public synchronized DigitalOut getDigitalOut(int ref) {
		this.parent.logWarn(this.log, "getDigitalOut is not implemented");
		this.parent._setProcessImageFault(true);
		return new SimpleDigitalOut(false);
	}

	@Override
	public synchronized int getDigitalOutCount() {
		this.parent.logWarn(this.log, "getDigitalOutCount is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

	@Override
	public synchronized DigitalIn[] getDigitalInRange(int offset, int count) {
		this.parent.logWarn(this.log, "getDigitalInRange is not implemented");
		this.parent._setProcessImageFault(true);
		var result = new DigitalIn[count];
		for (var i = 0; i < count; i++) {
			result[i] = new SimpleDigitalIn(false);
		}
		return result;
	}

	@Override
	public synchronized DigitalIn getDigitalIn(int ref) {
		this.parent.logWarn(this.log, "getDigitalInRange is not implemented");
		this.parent._setProcessImageFault(true);
		return new SimpleDigitalIn(false);
	}

	@Override
	public synchronized int getDigitalInCount() {
		this.parent.logWarn(this.log, "getDigitalInRange is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

	@Override
	public synchronized int getRegisterCount() {
		this.parent.logWarn(this.log, "getRegisterCount is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

	@Override
	public synchronized File getFile(int ref) {
		this.parent.logWarn(this.log, "getFile is not implemented");
		this.parent._setProcessImageFault(true);
		return null;
	}

	@Override
	public synchronized File getFileByNumber(int ref) {
		this.parent.logWarn(this.log, "getFileByNumber is not implemented");
		return null;
	}

	@Override
	public synchronized int getFileCount() {
		this.parent.logWarn(this.log, "getFileByNumber is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

	@Override
	public synchronized FIFO getFIFO(int ref) {
		this.parent.logWarn(this.log, "getFIFO is not implemented");
		this.parent._setProcessImageFault(true);
		return null;
	}

	@Override
	public synchronized FIFO getFIFOByAddress(int ref) {
		this.parent.logWarn(this.log, "getFIFOByAddress is not implemented");
		this.parent._setProcessImageFault(true);
		return null;
	}

	@Override
	public synchronized int getFIFOCount() {
		this.parent.logWarn(this.log, "getFIFOCount is not implemented");
		this.parent._setProcessImageFault(true);
		return 0;
	}

}
