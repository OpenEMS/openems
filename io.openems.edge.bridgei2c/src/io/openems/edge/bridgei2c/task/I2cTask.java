package io.openems.edge.bridgei2c.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;

public interface I2cTask {

   WriteChannel<Float> getFloatPowerLevel();

   void setFloatPowerLevel(float powerLevel) throws OpenemsError.OpenemsNamedException;

   int getPinPosition();

   //   int getOffset();
   //   int getPulseDuration();
   boolean isInverse();
   int calculateDigit(int digitRange);
   String getPwmModuleId();

}
