package io.openems.edge.kostal.enums;

import io.openems.common.types.OptionsEnum;

// 0x38 56 Inverter state 2 - U32 2 RO 0x03
public enum InverterState implements OptionsEnum {
  OFF(0, "Off"), //
  INIT(1, "Init"), //
  ISOMEAS(2, "IsoMeas"), //
  GRIDCHECK(3, "GridCheck"), //
  STARTUP(4, "StartUp"), //
  FEEDIN(6, "FeedIn"), //
  THROTTLED(7, "Throttled"), //
  EXTSWITCHOFF(8, "ExtSwitchOff"), //
  UPDATE(9, "Update"), //
  STANDBY(10, "Standby"), //
  GRIDSYNC(11, "GridSync"), //
  GRIDPRECHECK(12, "GridPreCheck"), //
  GRIDSWITCHOFF(13, "GridSwitchOff"), //
  OVERHEATING(14, "Overheating"), //
  SHUTDOWN(15, "Shutdown"), //
  IMPROPERDCVOLTAGE(16, "ImproperDcVoltage"), //
  ESB(17, "ESB"), //
  UNKNOWN(18, "Unknown"), //
  UNDEFINED(-1, "Undefined");

  private final int value;
  private final String name;

  private InverterState(int value, String name) {
    this.value = value;
    this.name = name;
  }

  @Override
  public int getValue() {
    return this.value;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public OptionsEnum getUndefined() {
    return UNDEFINED;
  }
}
