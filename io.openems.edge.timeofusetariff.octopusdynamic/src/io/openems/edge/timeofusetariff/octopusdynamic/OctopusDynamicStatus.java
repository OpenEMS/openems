package io.openems.edge.timeofusetariff.octopusdynamic;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Diagnostic channels for Octopus Dynamic GridBuy.
 * Timestamp channels contain Unix epoch seconds (UTC).
 */
public interface OctopusDynamicStatus extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER)),
        STATUS_AUTHENTICATION_FAILED(Doc.of(Level.WARNING)),
        STATUS_RATE_LIMITED(Doc.of(Level.WARNING)),
        STATUS_CONNECTION_FAILED(Doc.of(Level.WARNING)),
        STATUS_SERVER_ERROR(Doc.of(Level.WARNING)),
        LAST_SUCCESSFUL_UPDATE(Doc.of(OpenemsType.LONG)),
        FORECAST_VALID_UNTIL(Doc.of(OpenemsType.LONG)),
        NEXT_RETRY(Doc.of(OpenemsType.LONG));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setHttpStatusCode(int value) {
        this.channel(ChannelId.HTTP_STATUS_CODE).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setStatusAuthenticationFailed(boolean value) {
        this.channel(ChannelId.STATUS_AUTHENTICATION_FAILED).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setStatusRateLimited(boolean value) {
        this.channel(ChannelId.STATUS_RATE_LIMITED).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setStatusConnectionFailed(boolean value) {
        this.channel(ChannelId.STATUS_CONNECTION_FAILED).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setStatusServerError(boolean value) {
        this.channel(ChannelId.STATUS_SERVER_ERROR).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setLastSuccessfulUpdate(long value) {
        this.channel(ChannelId.LAST_SUCCESSFUL_UPDATE).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setForecastValidUntil(long value) {
        this.channel(ChannelId.FORECAST_VALID_UNTIL).setNextValue(value);
    }

    /** Updates the diagnostic value.
     *
     * @param value new diagnostic value
     */
    default void _setNextRetry(long value) {
        this.channel(ChannelId.NEXT_RETRY).setNextValue(value);
    }
}
