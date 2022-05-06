package io.openems.edge.batteryinverter.victron.rw;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.dccharger.victron.VictronDCCharger;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface VictronEss extends ManagedAsymmetricEss, ManagedSymmetricEss, AsymmetricEss, 
	SymmetricEss, ModbusSlave {

	void addCharger(VictronDCCharger charger);
	
	void removeCharger(VictronDCCharger VictronDCCharger);

	String getModbusBridgeId();

	Integer getUnitId();
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)),
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)),
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE))
		; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public enum Type {
		Multiplus2GX3kVa("Multiplus II-GX 3kVA Single Phase",-2400,2400,2400),
		Multiplus2GX5kVa("Multiplus II-GX 5kVA Single Phase",-4000,4000,4000),
		Multiplus2GX3kVaL1L2L3("Multiplus II-GX 3kVA Three Phase System", -2400*3,2400*3,2400*3),
		Multiplus2GX5kVaL1L2L3("Multiplus II-GX 5kVA Three Phase System", -4000*3,4000*3,4000*3);

		private int acInputLimit;
		private int acOutputLimit;
		private String displayName;
		private int apparentPowerLimit;

		Type(String displayName, int acInputLimit, int acOutputLimit, int apparentPowerLimit) {
			this.displayName = displayName;
			this.acInputLimit = acInputLimit;
			this.acOutputLimit = acOutputLimit;
			this.apparentPowerLimit = apparentPowerLimit;
		}

		public int getAcInputLimit() {
			return acInputLimit;
		}

		public int getAcOutputLimit() {
			return acOutputLimit;
		}

		public String getDisplayName() {
			return displayName;
		}

		public int getApparentPowerLimit() {
			return apparentPowerLimit;
		}
	}

}
