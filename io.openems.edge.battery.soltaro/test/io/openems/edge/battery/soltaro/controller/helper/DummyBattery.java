package io.openems.edge.battery.soltaro.controller.helper;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.single.versiona.ChargeIndication;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyBattery extends AbstractOpenemsComponent implements Battery {

	public static int DEFAULT_SOC = 50;
	public static int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static int DEFAULT_MAX_CELL_TEMPERATURE = 33;
	
	private int DEFAULT_CHARGE_INDICATION = 0;
	
	protected DummyBattery( //				
		) { //
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				DummyBattery.ChannelId.values() //
		);
			
			setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
			setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
			setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
			setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
			setSoc(DEFAULT_SOC);
			setChargeIndication(DEFAULT_CHARGE_INDICATION);
		}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this.getMinCellVoltage().setNextValue(null);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this.getMaxCellVoltage().setNextValue(maximalCellVoltage);
		this.getMaxCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltageToUndefined() {
		this.getMaxCellVoltage().setNextValue(null);
		this.getMaxCellVoltage().nextProcessImage();
	}
	
	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this.getMinCellTemperature().setNextValue(minimalCellTemperature);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMinimalCellTemperatureToUndefined() {
		this.getMinCellTemperature().setNextValue(null);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this.getMaxCellTemperature().setNextValue(maximalCellTemperature);
		this.getMaxCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperatureToUndefined() {
		this.getMaxCellTemperature().setNextValue(null);
		this.getMaxCellTemperature().nextProcessImage();
	}
	
	public void setChargeIndication(int chargeIndication) {
		this.getChargeIndication().setNextValue(chargeIndication);
		this.getChargeIndication().nextProcessImage();
	}

	public void setChargeIndicationToUndefined() {
		this.getChargeIndication().setNextValue(null);
		this.getChargeIndication().nextProcessImage();
	}
	
	public void setSoc(int soc) {
		this.getSoc().setNextValue(soc);
		this.getSoc().nextProcessImage();
	}

	public void setSocToUndefined() {
		this.getSoc().setNextValue(null);
		this.getSoc().nextProcessImage();
	}
	
	Channel<Integer> getChargeIndication() {
		return this.channel(ChannelId.CHARGE_INDICATION);
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {		
		CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
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
