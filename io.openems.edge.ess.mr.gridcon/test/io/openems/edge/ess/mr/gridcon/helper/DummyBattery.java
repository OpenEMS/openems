//package io.openems.edge.ess.mr.gridcon.helper;
//
//import io.openems.edge.battery.api.Battery;
//import io.openems.edge.common.component.AbstractOpenemsComponent;
//import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.ess.mr.gridcon.battery.ChargeIndication;
//import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
//
//public class DummyBattery extends AbstractOpenemsComponent implements Battery, SoltaroBattery {
//
//	public static int DEFAULT_SOC = 50;
//	public static int DEFAULT_MIN_CELL_VOLTAGE = 3280;
//	public static int DEFAULT_MAX_CELL_VOLTAGE = 3380;
//	public static int DEFAULT_MIN_CELL_TEMPERATURE = 25;
//	public static int DEFAULT_MAX_CELL_TEMPERATURE = 33;
//
//	private static final ChargeIndication DEFAULT_CHARGE_INDICATION = ChargeIndication.STANDING;
//
//	protected DummyBattery(//
//	) { //
//		super(//
//				OpenemsComponent.ChannelId.values(), //
//				Battery.ChannelId.values() //
//		);
//
//		setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
//		setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
//		setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
//		setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
//		setSoc(DEFAULT_SOC);
//		setChargeIndication(DEFAULT_CHARGE_INDICATION);
//	}
//
//	public void setMinimalCellVoltage(int minimalCellVoltage) {
//		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
//		this.getMinCellVoltage().nextProcessImage();
//	}
//
//	public void setMinimalCellVoltageToUndefined() {
//		this.getMinCellVoltage().setNextValue(null);
//		this.getMinCellVoltage().nextProcessImage();
//	}
//
//	public void setMaximalCellVoltage(int maximalCellVoltage) {
//		this.getMaxCellVoltage().setNextValue(maximalCellVoltage);
//		this.getMaxCellVoltage().nextProcessImage();
//	}
//
//	public void setMaximalCellVoltageToUndefined() {
//		this.getMaxCellVoltage().setNextValue(null);
//		this.getMaxCellVoltage().nextProcessImage();
//	}
//
//	public void setMinimalCellTemperature(int minimalCellTemperature) {
//		this.getMinCellTemperature().setNextValue(minimalCellTemperature);
//		this.getMinCellTemperature().nextProcessImage();
//	}
//
//	public void setMinimalCellTemperatureToUndefined() {
//		this.getMinCellTemperature().setNextValue(null);
//		this.getMinCellTemperature().nextProcessImage();
//	}
//
//	public void setMaximalCellTemperature(int maximalCellTemperature) {
//		this.getMaxCellTemperature().setNextValue(maximalCellTemperature);
//		this.getMaxCellTemperature().nextProcessImage();
//	}
//
//	public void setMaximalCellTemperatureToUndefined() {
//		this.getMaxCellTemperature().setNextValue(null);
//		this.getMaxCellTemperature().nextProcessImage();
//	}
//
//	public void setChargeIndication(ChargeIndication chargeIndication) {
//		this.getChargeIndication().setNextValue(chargeIndication);
//		this.getChargeIndication().nextProcessImage();
//	}
//
//	public void setChargeIndicationToUndefined() {
//		this.getChargeIndication().setNextValue(null);
//		this.getChargeIndication().nextProcessImage();
//	}
//
//	public void setSoc(int soc) {
//		this.getSoc().setNextValue(soc);
//		this.getSoc().nextProcessImage();
//	}
//
//	public void setSocToUndefined() {
//		this.getSoc().setNextValue(null);
//		this.getSoc().nextProcessImage();
//	}
//}
