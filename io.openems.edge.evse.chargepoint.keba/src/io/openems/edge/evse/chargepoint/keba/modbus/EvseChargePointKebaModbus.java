package io.openems.edge.evse.chargepoint.keba.modbus;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.keba.common.enums.ProductTypeAndFeatures;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePointKebaModbus extends EvseChargePoint, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PTAF_PRODUCT_TYPE(Doc.of(ProductTypeAndFeatures.ProductType.values())), //
		PTAF_CABLE_OR_SOCKET(Doc.of(ProductTypeAndFeatures.CableOrSocket.values())), //
		PTAF_SUPPORTED_CURRENT(Doc.of(ProductTypeAndFeatures.SupportedCurrent.values())), //
		PTAF_DEVICE_SERIES(Doc.of(ProductTypeAndFeatures.DeviceSeries.values())), //
		PTAF_ENERGY_METER(Doc.of(ProductTypeAndFeatures.EnergyMeter.values())), //
		PTAF_AUTHORIZATION(Doc.of(ProductTypeAndFeatures.Authorization.values())), //
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
}
