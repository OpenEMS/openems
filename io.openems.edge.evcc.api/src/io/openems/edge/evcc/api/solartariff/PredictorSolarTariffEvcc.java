package io.openems.edge.evcc.api.solartariff;

import org.osgi.service.event.EventHandler;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface PredictorSolarTariffEvcc extends OpenemsComponent, EventHandler {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        UNABLE_TO_PREDICT(Doc.of(Level.FAULT)),
        PREDICT_ENABLED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        PREDICT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS));

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
