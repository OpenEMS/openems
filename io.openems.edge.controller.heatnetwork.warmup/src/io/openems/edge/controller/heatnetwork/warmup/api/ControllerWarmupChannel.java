package io.openems.edge.controller.heatnetwork.warmup.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerWarmupChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        /**
         * Play or pause the heating program.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: boolean
         * <li>Unit: ON_OFF
         * </ul>
         */

        PLAY_PAUSE(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((BooleanWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Current temperature of the heating program. Primary output of the controller.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        WARMUP_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Get the total runtime length of the currently loaded heating program.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: Integer
         * <li>Unit: Minutes
         * </ul>
         */

        GET_LENGTH(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Get the elapsed time of the currently running heating program.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: Integer
         * <li>Unit: Minutes
         * </ul>
         */

        GET_ELAPSED_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Set current runtime of program to this minute.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: Integer
         * <li>Unit: Minutes
         * </ul>
         */

        GOTO_MINUTE(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((IntegerWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Load a heating program from file.
         * <ul>
         * <li>Interface: ControllerWarmupChannel
         * <li>Type: String
         * <li>Unit: None
         * </ul>
         */

        LOAD_FILE(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((StringWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * No error in this controller.
         * <ul>
         * <li>False if an Error occurred within this Controller.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        NO_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Play (true) or pause (false) the warmup controller program.
     * The warmup controller behaves like a media player. This is the play/pause button.
     * The controller has an auto resume feature. Upon activation, it will load the last running heating program and
     * load the elapsed time of that program. If the elapsed time is >0, the controller will automatically resume the
     * program. To do this, the controller will set this channel to "true".
     * If you want to stop a program prematurely and do not want it to auto resume next time the warmup controller is
     * activated, you need to set the goToMinuteWarmupProgram() channel to 0 after setting this channel to "false".
     * That is the equivalent of a stop button.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> playPauseWarmupController() {
        return this.channel(ChannelId.PLAY_PAUSE);
    }

    /**
     * Read the temperature of the warmup controller program. To output this number is the main purpose of the controller.
     * Unit is decimal degree celsius.
     *
     * @return the Channel
     */
    default Channel<Integer> getWarmupTemperature() {
        return this.channel(ChannelId.WARMUP_TEMPERATURE);
    }

    /**
     * The warmup controller behaves like a media player. This channel returns the total length of the warmup program loaded.
     * Unit is minutes.
     *
     * @return the Channel
     */
    default Channel<Integer> getLengthWarmupProgram() {
        return this.channel(ChannelId.GET_LENGTH);
    }

    /**
     * The warmup controller behaves like a media player. This channel returns the elapsed time of the warmup program
     * loaded. The elapsed time resets to 0 once a program has finished running.
     * Unit is minutes.
     *
     * @return the Channel
     */
    default Channel<Integer> getElapsedTimeWarmupProgram() {
        return this.channel(ChannelId.GET_ELAPSED_TIME);
    }

    /**
     * The warmup controller behaves like a media player. This channel forwards/rewinds the warmup program loaded
     * to the specified runtime. After execution, the controller will set this channel to -1.
     * Negative values are interpreted as "do nothing". Values equal to or higher than the program length (available
     * with getLengthWarmupProgram()) will stop the warmup controller.
     * The elapsed time of a running heating program is saved to a file and loaded on controller activation. The controller
     * automatically activates and resumes from the last saved position if the elapsed time is >0. To prevent the auto
     * resume, pause the controller and set the goToMinuteWarmupProgram() channel to 0 before deactivating the module.
     * Unit is minutes.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> goToMinuteWarmupProgram() {
        return this.channel(ChannelId.GOTO_MINUTE);
    }

    /**
     * This channel loads a warmup program from the specified file path. Will only execute when controller is paused.
     * Does NOT reset the runtime, so call the GoToMinuteWarmupProgram() with 0 before play to start the program from
     * the beginning. The idea behind the no runtime reset is to make temperature edits of a running program less of
     * a hassle. Simply edit the temperatures in the config file and load the edited file with this function.
     * The channel will return "done" if the file was successfully loaded or "failed" if not.
     *
     * @return the Channel
     */
    default WriteChannel<String> loadWarmupProgram() { return this.channel(ChannelId.LOAD_FILE); }

    /**
     * Is true when no error has occurred.
     *
     * @return the Channel
     */

    default Channel<Boolean> noError() {
        return this.channel(ChannelId.NO_ERROR);
    }

}
