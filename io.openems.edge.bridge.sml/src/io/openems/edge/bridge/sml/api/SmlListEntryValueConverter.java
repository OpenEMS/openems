package io.openems.edge.bridge.sml.api;

import org.openmuc.jsml.EUnit;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlUnit;
import org.openmuc.jsml.structures.Unsigned8;

public class SmlListEntryValueConverter {
	private SmlListEntry smlListEntry;
	private String convertedString = "";
	private boolean convertedBoolean = false;
	private double convertedDouble = 0.0;
	private byte[] obisCode = null;
	private SmlUnit unit = new SmlUnit(new Unsigned8(EUnit.EMPTY.id()));
	private String dataType = "";
	private int precision = 0;

	public SmlListEntryValueConverter(SmlListEntry smlListEntry) {
		this.smlListEntry = smlListEntry;
		this.Convert();
	}

	private boolean Convert() {
		var success = false;
		if (null == this.smlListEntry.getValue()) { // do not crash on null value
			return success;
		}

		var dataTypeArray = this.smlListEntry.getValue().getDatatype().split("\\.");
		this.dataType = dataTypeArray[dataTypeArray.length - 1];

		if (this.dataType.contains("OctetString")) {
			obisCode = this.smlListEntry.getObjName().toBytes();
			this.convertedString = this.smlListEntry.getValue().getChoice().toString();
			success = true;
		} else if (this.dataType.contains("SmlBoolean")) {
			obisCode = this.smlListEntry.getObjName().toBytes();
			this.convertedBoolean = Boolean.parseBoolean(this.smlListEntry.getValue().getChoice().toString());
			this.dataType = "SmlBoolean";
			success = true;
		} else if (this.dataType.contains("Integer8") || this.dataType.contains("Integer16")
				|| this.dataType.contains("Integer32") || this.dataType.contains("Integer64")
				|| this.dataType.contains("Unsigned8") || this.dataType.contains("Unsigned16")
				|| this.dataType.contains("Unsigned32") || this.dataType.contains("Unsigned64")) {
			var doubleVal = Double.parseDouble(this.smlListEntry.getValue().getChoice().toString());
			var scaler = this.smlListEntry.getScaler().getIntVal();
			this.precision = -scaler;
			if (this.precision < 0)
				this.precision = 0;
			doubleVal = doubleVal * Math.pow(10, scaler);

			int unit = EUnit.EMPTY.id();
			if (null != this.smlListEntry.getUnit())
				unit = this.smlListEntry.getUnit().getVal();

			obisCode = this.smlListEntry.getObjName().toBytes();
			this.convertedDouble = doubleVal;
			this.unit = new SmlUnit(new SmlUnit(new Unsigned8(unit)));
			success = true;
		}
		return success;
	}

	public String getConvertedString() {
		return convertedString;
	}

	public boolean getConvertedBoolean() {
		return convertedBoolean;
	}

	public double getConvertedDouble() {
		return convertedDouble;
	}

	public byte[] getObisCode() {
		return obisCode;
	}

	public SmlUnit getUnit() {
		return unit;
	}
	
	public String getDataType() {
		return dataType;
	}
}
