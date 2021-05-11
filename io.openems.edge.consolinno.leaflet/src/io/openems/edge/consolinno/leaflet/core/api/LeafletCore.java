package io.openems.edge.consolinno.leaflet.core.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;


/**
 * This provides the Main Leaflet Module for the Consolinno Leaflet Stack.
 * Configures various Modules and Provides a central hub for common methods.
 */
@ProviderType
public interface LeafletCore extends OpenemsComponent {

    /**
     * Check if the Module that is trying to activate is physically present.
     *
     * @param moduleType   TMP,RELAY,PWM
     * @param moduleNumber Internal Number of the module
     * @param mReg     Pin position of the Module
     * @param id           Unique Id of the Device
     * @return boolean true if present
     */
    boolean modbusModuleCheckout(ModuleType moduleType, int moduleNumber, int mReg, String id);

    /**
     * Returns the Address from the Source file which is usually needed for operation.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param mReg         Usually the Position of the device but sometimes position-1. Check Register map
     * @return Modbus Offset as integer
     */
    int getFunctionAddress(ModuleType moduleType, int moduleNumber, int mReg);

    /**
     * Return the Address from the Source file which is usually needed for operation.
     * This method is only for the AIO module.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param position     Pin position of the device on the module
     * @param input        true if the Register needed is an input Register | false if an output
     * @return Modbus Offset as integer
     */
    //true = input | false = output
    int getFunctionAddress(ModuleType moduleType, int moduleNumber, int position, boolean input);

    /**
     * Returns the Address from the Source file of the configuration register.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @return Configuration Register address for the module
     */
    int getConfigurationAddress(ModuleType moduleType, int moduleNumber);

    /**
     * Removes Module from internal Position map.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param position     Pin position of the device on the module
     */
    void removeModule(ModuleType moduleType, int moduleNumber, int position);

    /**
     * Invert the Relay Functionality.
     *
     * @param moduleNumber Module Number specified on the Relay module
     * @param position     Position of the Relay on the Module
     */
    void invertRelay(int moduleNumber, int position);

    /**
     * Sets the Frequency of a Pwm Module.
     *
     * @param moduleNumber Module Number of the Pwm Module that is getting configured
     * @param frequency    Frequency value (between 24 and 1500hz)
     */
    void setPwmConfiguration(int moduleNumber, int frequency);

    /**
     * Returns the Register Address for the Discrete Output needed for the inversion of a Pwm Module.
     *
     * @param pwmModule Module number specified on the Pwm Module
     * @param mReg      Pin Position of the Pwm Device
     * @return Invert Register for the Pwm Device
     */
    int getPwmDiscreteOutputAddress(int pwmModule, int mReg);

    /**
     * Configures the AIO Modules.
     *
     * @param moduleNumber Module number specified on the Aio Module
     * @param position     Pin Position of the Aio Device
     * @param config       The configuration, of the specific AIO Output (e.g. 0-20mA_in)
     */
    void setAioConfig(int moduleNumber, int position, String config);

    /**
     * Return the Register Address where the AIO module outputs a percentage value of whatever is configured.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param mReg         Position of the AIO device
     * @param input        true if Input | false if output
     * @return Address of the Percent conversion Register
     */
    int getAioPercentAddress(ModuleType moduleType, int moduleNumber, int mReg, boolean input);

    /**
     * Puts a relay back in regular mode. Is Called when a inverted Relay deactivates.
     *
     * @param moduleNumber Module number specified on the Device
     * @param position     Position of the Relay on the module
     */
    void revertInversion(int moduleNumber, int position);

    /**
     * Checks if the Firmware version is at least the minimum required version for the Configurator to run properly.
     *
     * @return true if the Firmware is compatible
     */
    boolean checkFirmwareCompatibility();


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
        AIO_MODULES(Doc.of(OpenemsType.INTEGER)),
        TEMPERATURE_MODULES(Doc.of(OpenemsType.INTEGER)),
        RELAY_MODULES(Doc.of(OpenemsType.INTEGER)),
        PWM_MODULES(Doc.of(OpenemsType.INTEGER)),
        /**
         * Internal Error Channel.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Boolean
         * </ul>
         */
        ERROR(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Write Configuration Channels for the Inversion of the Relays.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        WRITE_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Write Configuration Channels for the Frequency of the Pwm Modules.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        WRITE_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Write Configuration Channels for the Operating modes of the Aio Modules.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        WRITE_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Read-Only Channels for the Configured Pwm Frequency.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        READ_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER)),
        READ_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER)),
        /**
         * Read-Only Channels for the Configured Aio Operation Modes.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        READ_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER)),
        READ_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER)),
        /**
         * Read-Only Channels for the Configured Relay Inversion.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        READ_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        READ_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER)),
        /**
         * Write Channel for the Leaflet Configuration.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
        WRITE_LEAFLET_CONFIG(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Read-Only Channels for the Leaflet Configuration.
         * <ul>
         * <li>Interface: LeafletConfigurator
         * <li>Type: Integer
         * </ul>
         */
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

    /**
     * Return channel for internal Errors.
     *
     * @return Boolean Error Channel
     */
    default Channel<Boolean> getErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    /**
     * Return channel for the detected Temperature Modules.
     *
     * @return Integer TemperatureModules Channel
     */
    default Channel<Integer> getTemperatureModulesChannel() {
        return this.channel(ChannelId.TEMPERATURE_MODULES);
    }

    /**
     * Return WriteChannel for the Leaflet Config.
     *
     * @return Integer WriteChannel WriteLeafletConfig
     */
    default WriteChannel<Integer> getWriteLeafletConfigChannel() {
        return this.channel(ChannelId.WRITE_LEAFLET_CONFIG);
    }

    /**
     * Return channel for the detected Aio Modules.
     *
     * @return Integer  AioModules Channel
     */
    default Channel<Integer> getAioModulesChannel() {
        return this.channel(ChannelId.AIO_MODULES);
    }

    /**
     * Return detected Aio Modules.
     *
     * @return Integer of detected Aio Modules
     */
    default int getAioModules() {
        if (this.getAioModulesChannel().value().isDefined()) {
            return this.getAioModulesChannel().value().get();
        } else if (this.getAioModulesChannel().getNextValue().isDefined()) {
            return this.getAioModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    /**
     * Return detected Temperature Modules.
     *
     * @return Integer of detected Temperature Modules
     */
    default int getTemperatureModules() {
        if (this.getTemperatureModulesChannel().value().isDefined()) {
            return this.getTemperatureModulesChannel().value().get();
        } else if (this.getTemperatureModulesChannel().getNextValue().isDefined()) {
            return this.getTemperatureModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    /**
     * Return channel for the detected Relay Modules.
     *
     * @return Integer RelayModules Channel
     */
    default Channel<Integer> getRelayModulesChannel() {
        return this.channel(ChannelId.RELAY_MODULES);
    }

    /**
     * Return detected Relay Modules.
     *
     * @return Integer of detected Relay Modules
     */
    default int getRelayModules() {
        if (this.getRelayModulesChannel().value().isDefined()) {
            return this.getRelayModulesChannel().value().get();
        } else if (this.getRelayModulesChannel().getNextValue().isDefined()) {
            return this.getRelayModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    /**
     * Return channel for the detected Pwm Modules.
     *
     * @return Integer PwmModules Channel
     */
    default Channel<Integer> getPwmModulesChannel() {
        return this.channel(ChannelId.PWM_MODULES);
    }

    /**
     * Return detected Pwm Modules.
     *
     * @return Integer of detected Pwm Modules
     */
    default int getPwmModules() {
        if (this.getPwmModulesChannel().value().isDefined()) {
            return this.getPwmModulesChannel().value().get();
        } else if (this.getPwmModulesChannel().getNextValue().isDefined()) {
            return this.getPwmModulesChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    /**
     * Return WriteChannel for the Configuration of the first Aio Module.
     *
     * @return Integer AioConfigOne WriteChannel
     */
    default WriteChannel<Integer> getAioConfigOne() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_ONE);
    }

    /**
     * Return WriteChannel for the Configuration of the second Aio Module.
     *
     * @return Integer AioConfigTwo WriteChannel
     */
    default WriteChannel<Integer> getAioConfigTwo() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_TWO);
    }

    /**
     * Return WriteChannel for the Configuration of the third Aio Module.
     *
     * @return Integer AioConfigThree WriteChannel
     */
    default WriteChannel<Integer> getAioConfigThree() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_THREE);
    }

    /**
     * Return WriteChannel for the Configuration of the forth Aio Module.
     *
     * @return Integer AioConfigFour WriteChannel
     */
    default WriteChannel<Integer> getAioConfigFour() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_FOUR);
    }

    /**
     * Return WriteChannel for the Configuration of the fifth Aio Module.
     *
     * @return Integer AioConfigFive WriteChannel
     */
    default WriteChannel<Integer> getAioConfigFive() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_FIVE);
    }

    /**
     * Return WriteChannel for the Configuration of the sixth Aio Module.
     *
     * @return Integer AioConfigSix WriteChannel
     */
    default WriteChannel<Integer> getAioConfigSix() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_SIX);
    }

    /**
     * Return WriteChannel for the Configuration of the seventh Aio Module.
     *
     * @return Integer AioConfigSeven WriteChannel
     */
    default WriteChannel<Integer> getAioConfigSeven() {
        return this.channel(ChannelId.WRITE_AIO_CONFIG_SEVEN);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the first Relay Module.
     *
     * @return Integer WriteInvertRelayOneStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayOneStatus() {
        return this.channel(ChannelId.WRITE_RELAY_ONE_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the second Relay Module.
     *
     * @return Integer WriteInvertRelayTwoStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayTwoStatus() {
        return this.channel(ChannelId.WRITE_RELAY_TWO_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the third Relay Module.
     *
     * @return Integer WriteInvertRelayThreeStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayThreeStatus() {
        return this.channel(ChannelId.WRITE_RELAY_THREE_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the forth Relay Module.
     *
     * @return Integer WriteInvertRelayFourStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayFourStatus() {
        return this.channel(ChannelId.WRITE_RELAY_FOUR_INVERT_STATUS);
    }

    /**
     * Return the Configuration of the Aio Modules.
     *
     * @return Integer of Aio Module Configuration
     */
    default Integer getReadAioConfig() {
        int result = 0;
        if (this.getReadAioConfigOne().value().isDefined()) {
            result = result | this.getReadAioConfigOne().value().get();
        } else if (this.getReadAioConfigOne().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigOne().getNextValue().get();
        }
        if (this.getReadAioConfigTwo().value().isDefined()) {
            result = result | this.getReadAioConfigTwo().value().get();
        } else if (this.getReadAioConfigTwo().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigTwo().getNextValue().get();
        }
        if (this.getReadAioConfigThree().value().isDefined()) {
            result = result | this.getReadAioConfigThree().value().get();
        } else if (this.getReadAioConfigThree().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigThree().getNextValue().get();
        }
        if (this.getReadAioConfigFour().value().isDefined()) {
            result = result | this.getReadAioConfigFour().value().get();
        } else if (this.getReadAioConfigFour().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigFour().getNextValue().get();
        }
        if (this.getReadAioConfigFive().value().isDefined()) {
            result = result | this.getReadAioConfigFive().value().get();
        } else if (this.getReadAioConfigFive().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigFive().getNextValue().get();
        }
        if (this.getReadAioConfigSix().value().isDefined()) {
            result = result | this.getReadAioConfigSix().value().get();
        } else if (this.getReadAioConfigSix().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigSix().getNextValue().get();
        }
        if (this.getReadAioConfigSeven().value().isDefined()) {
            result = result | this.getReadAioConfigSeven().value().get();
        } else if (this.getReadAioConfigSeven().getNextValue().isDefined()) {
            result = result | this.getReadAioConfigSeven().getNextValue().get();
        }

        return result;
    }

    /**
     * Returns the Config Channel for the first Aio Module.
     *
     * @return Integer ReadAioConfigOne Channel
     */
    default Channel<Integer> getReadAioConfigOne() {
        return this.channel(ChannelId.READ_AIO_CONFIG_ONE);
    }

    /**
     * Returns the Config Channel for the second Aio Module.
     *
     * @return Integer ReadAioConfigTwo Channel
     */
    default Channel<Integer> getReadAioConfigTwo() {
        return this.channel(ChannelId.READ_AIO_CONFIG_TWO);
    }

    /**
     * Returns the Config Channel for the third Aio Module.
     *
     * @return Integer ReadAioConfigThree Channel
     */
    default Channel<Integer> getReadAioConfigThree() {
        return this.channel(ChannelId.READ_AIO_CONFIG_THREE);
    }

    /**
     * Returns the Config Channel for the forth Aio Module.
     *
     * @return Integer ReadAioConfigFour Channel
     */
    default Channel<Integer> getReadAioConfigFour() {
        return this.channel(ChannelId.READ_AIO_CONFIG_FOUR);
    }

    /**
     * Returns the Config Channel for the fifth Aio Module.
     *
     * @return Integer ReadAioConfigFive Channel
     */
    default Channel<Integer> getReadAioConfigFive() {
        return this.channel(ChannelId.READ_AIO_CONFIG_FIVE);
    }

    /**
     * Returns the Config Channel for the sixth Aio Module.
     *
     * @return Integer ReadAioConfigSix Channel
     */
    default Channel<Integer> getReadAioConfigSix() {
        return this.channel(ChannelId.READ_AIO_CONFIG_SIX);
    }

    /**
     * Returns the Config Channel for the seventh Aio Module.
     *
     * @return Integer ReadAioConfigSeven Channel
     */
    default Channel<Integer> getReadAioConfigSeven() {
        return this.channel(ChannelId.READ_AIO_CONFIG_SEVEN);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the first Module.
     *
     * @return Integer WritePwmFrequencyOne WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyOne() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_ONE);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the second Module.
     *
     * @return Integer WritePwmFrequencyTwo WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyTwo() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_TWO);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the third Module.
     *
     * @return Integer WritePwmFrequencyThree WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyThree() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_THREE);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the forth Module.
     *
     * @return Integer WritePwmFrequencyFour WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyFour() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_FOUR);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the fifth Module.
     *
     * @return Integer WritePwmFrequencyFive WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyFive() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_FIVE);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the sixth Module.
     *
     * @return Integer WritePwmFrequencySix WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencySix() {

        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_SIX);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the seventh Module.
     *
     * @return Integer WritePwmFrequencySeven WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencySeven() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_SEVEN);
    }

    /**
     * Return the Configuration Channel for the Pwm Frequency of the eighth Module.
     *
     * @return Integer WritePwmFrequencyEight WriteChannel
     */
    default WriteChannel<Integer> getWritePwmFrequencyEight() {
        return this.channel(ChannelId.WRITE_PWM_FREQUENCY_EIGHT);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the first Module.
     *
     * @return Integer ReadPwmFrequencyOne Channel
     */
    default Channel<Integer> getReadPwmFrequencyOne() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_ONE);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the second Module.
     *
     * @return Integer ReadPwmFrequencyTwo Channel
     */
    default Channel<Integer> getReadPwmFrequencyTwo() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_TWO);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the third Module.
     *
     * @return Integer ReadPwmFrequencyThree Channel
     */
    default Channel<Integer> getReadPwmFrequencyThree() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_THREE);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the forth Module.
     *
     * @return Integer ReadPwmFrequencyFour Channel
     */
    default Channel<Integer> getReadPwmFrequencyFour() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_FOUR);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the fifth Module.
     *
     * @return Integer ReadPwmFrequencyFive Channel
     */
    default Channel<Integer> getReadPwmFrequencyFive() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_FIVE);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the sixth Module.
     *
     * @return Integer ReadPwmFrequencySix Channel
     */
    default Channel<Integer> getReadPwmFrequencySix() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_SIX);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the seventh Module.
     *
     * @return Integer ReadPwmFrequencySeven Channel
     */
    default Channel<Integer> getReadPwmFrequencySeven() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_SEVEN);
    }

    /**
     * Return the Read-Only Configuration Channel for the Pwm Frequency of the eighth Module.
     *
     * @return Integer ReadPwmFrequencyEight Channel
     */
    default Channel<Integer> getReadPwmFrequencyEight() {
        return this.channel(ChannelId.READ_PWM_FREQUENCY_EIGHT);
    }

    /**
     * Return the Configured Pwm Frequency for all modules.
     *
     * @return String of Pwm Frequency
     */
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