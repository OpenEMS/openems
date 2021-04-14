package io.openems.edge.consolinno.modbus.configurator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface LeafletConfigurator extends OpenemsComponent {
    boolean modbusModuleCheckout(ModuleType moduleType, int moduleNumber, int position, String id);


    int getFunctionAddress(ModuleType moduleType, int moduleNumber, int position);

    //true = input | false = output
    int getFunctionAddress(ModuleType moduleType, int moduleNumber, int position, boolean b);

    int getConfigurationAddress(ModuleType temp, int moduleNumber);

    void removeModule(ModuleType type, int moduleNumber, int position);

    void invertRelay(int moduleNumber, int position);


    void setPwmConfiguration(int moduleNumber, int frequency);

    int getPwmDiscreteOutputAddress(int pwmModule, int mReg);

    void setAioConfig(int aioModule, int inputMReg, String config);

    int getAioPercentAddress(ModuleType type, int moduleNumber, int mReg, boolean b);

    void revertInversion(int moduleNumber, int position);


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Temperature modules.
         * C:Connected | D:Disconnected
         * T1:TMP Module 1 | T2:TMP Module 2 | T3:TMP Module 3
         * 0b111 (7) : T1:C | T2:C | T3:C
         * 0b110 (6) : T1:D | T2:C | T3:C
         * 0b101 (5) : T1:C | T2:D | T3:C
         * 0b100 (4) : T1:D | T2:D | T3:C
         * 0b011 (3) : T1:C | T2:C | T3:D
         * 0b010 (2) : T1:D | T2:C | T3:D
         * 0b001 (1) : T1:C | T2:D | T3:D
         * 0b000 (0) : T1:D | T2:D | T3:D
         * --------------------------------
         * Relay modules.
         * C:Connected | D:Disconnected
         * R1:Relay Module 1 | R2:Relay Module 2 | R3:Relay Module 3 | R4:Relay Module 4
         * 0b1111 (15): R1:C | R2:C | R3:C | R4:C
         * 0b1110 (14): R1:D | R2:C | R3:C | R4:C
         * 0b1101 (13): R1:C | R2:D | R3:C | R4:C
         * 0b1100 (12): R1:D | R2:D | R3:C | R4:C
         * 0b1011 (11): R1:C | R2:C | R3:D | R4:C
         * 0b1010 (10): R1:D | R2:C | R3:D | R4:C
         * 0b1001 (9) : R1:C | R2:D | R3:D | R4:C
         * 0b1000 (8) : R1:D | R2:D | R3:D | R4:C
         * 0b0111 (7) : R1:C | R2:C | R3:C | R4:D
         * 0b0110 (6) : R1:D | R2:C | R3:C | R4:D
         * 0b0101 (5) : R1:C | R2:D | R3:C | R4:D
         * 0b0100 (4) : R1:D | R2:D | R3:C | R4:D
         * 0b0011 (3) : R1:C | R2:C | R3:D | R4:D
         * 0b0010 (2) : R1:D | R2:C | R3:D | R4:D
         * 0b0001 (1) : R1:C | R2:D | R3:D | R4:D
         * 0b0000 (0) : R1:D | R2:D | R3:D | R4:D
         * --------------------------------
         * PWM modules.
         * C:Connected | D:Disconnected
         * Pn: Pwm Module n
         * 0b11011011 =
         * P8:C|P7:C|P6:D|P5:C|P4:C|P3:D|P2:C|P1:C
         * |    |    |    |    |    |    |    |
         * V    V    V    V    V    V    V    V
         * 1    1    0    1    1    0    1    1
         * --------------------------------
         *
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        ERROR(Doc.of(OpenemsType.BOOLEAN)),
        WRITE_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        READ_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        AIO_MODULES(Doc.of(OpenemsType.INTEGER)),
        TEMPERATURE_MODULES(Doc.of(OpenemsType.INTEGER)),
        RELAY_MODULES(Doc.of(OpenemsType.INTEGER)),
        PWM_MODULES(Doc.of(OpenemsType.INTEGER)),
        WRITE_LEAFLET_CONFIG(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        READ_LEAFLET_CONFIG(Doc.of(OpenemsType.INTEGER));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    enum ModuleType {
        TMP,
        REL,
        PWM,
        LEAFLET,
        AIO,
        ERROR;

        public static boolean contains(String manage) {
            for (ModuleType moduleType : ModuleType.values()) {
                if (moduleType.name().equals(manage)) {
                    return true;
                }
            }
            return false;
        }
    }

    default Channel<Boolean> getErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    default Channel<Integer> getTemperatureModulesChannel() {
        return this.channel(ChannelId.TEMPERATURE_MODULES);
    }

    default WriteChannel<Integer> getWriteLeafletConfigChannel() {
        return this.channel(ChannelId.WRITE_LEAFLET_CONFIG);
    }

    default Channel<Integer> getAioModulesChannel() {
        return this.channel(ChannelId.AIO_MODULES);
    }

    default int getAioModules() {
        if (this.getAioModulesChannel().value().isDefined()) {
            return this.getAioModulesChannel().value().get();
        } else if (this.getAioModulesChannel().getNextValue().isDefined()) {
            return this.getAioModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    default int getTemperatureModules() {
        if (this.getTemperatureModulesChannel().value().isDefined()) {
            return this.getTemperatureModulesChannel().value().get();
        } else if (this.getTemperatureModulesChannel().getNextValue().isDefined()) {
            return this.getTemperatureModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    default Channel<Integer> getRelayModulesChannel() {
        return this.channel(ChannelId.RELAY_MODULES);
    }

    default int getRelayModules() {
        if (this.getRelayModulesChannel().value().isDefined()) {
            return this.getRelayModulesChannel().value().get();
        } else if (this.getRelayModulesChannel().getNextValue().isDefined()) {
            return this.getRelayModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    default Channel<Integer> getPwmModulesChannel() {
        return this.channel(ChannelId.PWM_MODULES);
    }

    default int getPwmModules() {
        if (this.getPwmModulesChannel().value().isDefined()) {
            return this.getPwmModulesChannel().value().get();
        } else if (this.getPwmModulesChannel().getNextValue().isDefined()) {
            return this.getPwmModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    default WriteChannel<Integer> getAioConfigOne() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_ONE);
    }

    default WriteChannel<Integer> getAioConfigTwo() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_TWO);
    }

    default WriteChannel<Integer> getAioConfigThree() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_THREE);
    }

    default WriteChannel<Integer> getAioConfigFour() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_FOUR);
    }

    default WriteChannel<Integer> getAioConfigFive() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_FIVE);
    }

    default WriteChannel<Integer> getAioConfigSix() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_SIX);
    }

    default WriteChannel<Integer> getAioConfigSeven() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_SEVEN);
    }

    default WriteChannel<Integer> getWriteInvertRelayOneStatus() {
        return this.channel(ChannelId.WRITE_RELAY_ONE_INVERT_STATUS);
    }

    default WriteChannel<Integer> getWriteInvertRelayTwoStatus() {
        return this.channel(ChannelId.WRITE_RELAY_TWO_INVERT_STATUS);
    }

    default WriteChannel<Integer> getWriteInvertRelayThreeStatus() {
        return this.channel(ChannelId.WRITE_RELAY_THREE_INVERT_STATUS);
    }

    default WriteChannel<Integer> getWriteInvertRelayFourStatus() {
        return this.channel(ChannelId.WRITE_RELAY_FOUR_INVERT_STATUS);
    }

    default Integer getReadAioConfig() {
        int result = 0;
        if (getReadAioConfigOne().value().isDefined()) {
            result = result | getReadAioConfigOne().value().get();
        } else if (getReadAioConfigOne().getNextValue().isDefined()) {
            result = result | getReadAioConfigOne().getNextValue().get();
        }
        if (getReadAioConfigTwo().value().isDefined()) {
            result = result | getReadAioConfigTwo().value().get();
        } else if (getReadAioConfigTwo().getNextValue().isDefined()) {
            result = result | getReadAioConfigTwo().getNextValue().get();
        }
        if (getReadAioConfigThree().value().isDefined()) {
            result = result | getReadAioConfigThree().value().get();
        } else if (getReadAioConfigThree().getNextValue().isDefined()) {
            result = result | getReadAioConfigThree().getNextValue().get();
        }
        if (getReadAioConfigFour().value().isDefined()) {
            result = result | getReadAioConfigFour().value().get();
        } else if (getReadAioConfigFour().getNextValue().isDefined()) {
            result = result | getReadAioConfigFour().getNextValue().get();
        }
        if (getReadAioConfigFive().value().isDefined()) {
            result = result | getReadAioConfigFive().value().get();
        } else if (getReadAioConfigFive().getNextValue().isDefined()) {
            result = result | getReadAioConfigFive().getNextValue().get();
        }
        if (getReadAioConfigSix().value().isDefined()) {
            result = result | getReadAioConfigSix().value().get();
        } else if (getReadAioConfigSix().getNextValue().isDefined()) {
            result = result | getReadAioConfigSix().getNextValue().get();
        }
        if (getReadAioConfigSeven().value().isDefined()) {
            result = result | getReadAioConfigSeven().value().get();
        } else if (getReadAioConfigSeven().getNextValue().isDefined()) {
            result = result | getReadAioConfigSeven().getNextValue().get();
        }

        return result;
    }

    default Channel<Integer> getReadAioConfigOne() {
        return this.channel(ChannelId.READ_AIO_CONFIG_ONE);
    }

    default Channel<Integer> getReadAioConfigTwo() {
        return this.channel(ChannelId.READ_AIO_CONFIG_TWO);
    }

    default Channel<Integer> getReadAioConfigThree() {
        return this.channel(ChannelId.READ_AIO_CONFIG_THREE);
    }

    default Channel<Integer> getReadAioConfigFour() {
        return this.channel(ChannelId.READ_AIO_CONFIG_FOUR);
    }

    default Channel<Integer> getReadAioConfigFive() {
        return this.channel(ChannelId.READ_AIO_CONFIG_FIVE);
    }

    default Channel<Integer> getReadAioConfigSix() {
        return this.channel(ChannelId.READ_AIO_CONFIG_SIX);
    }

    default Channel<Integer> getReadAioConfigSeven() {
        return this.channel(ChannelId.READ_AIO_CONFIG_SEVEN);
    }

    default WriteChannel<Integer> getWritePwmFrequencyOne() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_ONE);
    }

    default WriteChannel<Integer> getWritePwmFrequencyTwo() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_TWO);
    }

    default WriteChannel<Integer> getWritePwmFrequencyThree() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_THREE);
    }

    default WriteChannel<Integer> getWritePwmFrequencyFour() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_FOUR);
    }

    default WriteChannel<Integer> getWritePwmFrequencyFive() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_FIVE);
    }

    default WriteChannel<Integer> getWritePwmFrequencySix() {

        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_SIX);
    }

    default WriteChannel<Integer> getWritePwmFrequencySeven() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_SEVEN);
    }

    default WriteChannel<Integer> getWritePwmFrequencyEight() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_EIGHT);
    }

    default Channel<Integer> getReadPwmFrequencyOne() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_ONE);
    }

    default Channel<Integer> getReadPwmFrequencyTwo() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_TWO);
    }

    default Channel<Integer> getReadPwmFrequencyThree() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_THREE);
    }

    default Channel<Integer> getReadPwmFrequencyFour() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_FOUR);
    }

    default Channel<Integer> getReadPwmFrequencyFive() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_FIVE);
    }

    default Channel<Integer> getReadPwmFrequencySix() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_SIX);
    }

    default Channel<Integer> getReadPwmFrequencySeven() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_SEVEN);
    }

    default Channel<Integer> getReadPwmFrequencyEight() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_EIGHT);
    }

    default String getReadPwmFrequency() {

        String returnString = "";
        if (this.getReadPwmFrequencyOne().value().isDefined() && this.getReadPwmFrequencyOne().value().get() != -1
                && this.getReadPwmFrequencyOne().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyOne().value();
        }
        if (this.getReadPwmFrequencyTwo().value().isDefined() && this.getReadPwmFrequencyTwo().value().get() != -1
                && this.getReadPwmFrequencyTwo().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyTwo().value();
        }
        if (this.getReadPwmFrequencyThree().value().isDefined() && this.getReadPwmFrequencyThree().value().get() != -1
                && this.getReadPwmFrequencyThree().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyThree().value();
        }
        if (this.getReadPwmFrequencyFour().value().isDefined() && this.getReadPwmFrequencyFour().value().get() != -1
                && this.getReadPwmFrequencyFour().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyFour().value();
        }
        if (this.getReadPwmFrequencyFive().value().isDefined() && this.getReadPwmFrequencyFive().value().get() != -1
                && this.getReadPwmFrequencyFive().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyFive().value();
        }
        if (this.getReadPwmFrequencySix().value().isDefined() && this.getReadPwmFrequencySix().value().get() != -1
                && this.getReadPwmFrequencySix().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencySix().value();
        }
        if (this.getReadPwmFrequencySeven().value().isDefined() && this.getReadPwmFrequencySeven().value().get() != -1
                && this.getReadPwmFrequencySeven().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencySeven().value();
        }
        if (this.getReadPwmFrequencyEight().value().isDefined() && this.getReadPwmFrequencyEight().value().get() != -1
                && this.getReadPwmFrequencyEight().value().get() != 65535) {
            returnString = returnString + this.getReadPwmFrequencyEight().value();
        }
        return returnString;
    }
}