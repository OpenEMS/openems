package io.openems.edge.kostal.enums;

import io.openems.common.types.OptionsEnum;

// 0xCA 202 PSSB fuse state 5 - Float 2 RO 0x03
public enum FuseState implements OptionsEnum {
  FUSE_FAIL(0x00, "Fuse fail"), //
  FUSE_OK(0x01, "Fuse ok"), //
  UNCHECKED(0xFF, "Unchecked"), //
  UNDEFINED(-1, "Undefined");

  private final int value;
  private final String name;

  private FuseState(int value, String name) {
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
