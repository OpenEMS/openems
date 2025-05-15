package io.openems.edge.kostal.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryManagementMode implements OptionsEnum {
  NO_EXTERNAL_BATTERY_MANAGEMENT(0x00, "No external battery management"), //
  EXTERNAL_BATTERY_MANAGEMENT_DIGITAL_IO(
    0x01,
    "External battery management via digital I/O"
  ), //
  EXTERNAL_BATTERY_MANAGEMENT_MODBUS(
    0x02,
    "External battery management via MODBUS protocol"
  ), //
  UNDEFINED(-1, "Undefined");

  private final int value;
  private final String name;

  private BatteryManagementMode(int value, String name) {
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
