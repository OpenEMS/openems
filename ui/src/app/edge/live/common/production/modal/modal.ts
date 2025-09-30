import { Component } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, EdgeConfig, Utils } from "src/app/shared/shared";

@Component({
    templateUrl: "./MODAL.HTML",
    standalone: false,
})
export class ModalComponent extends AbstractModal {

    // reference to the Utils method to access via html
    public readonly isLastElement = UTILS.IS_LAST_ELEMENT;
    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

    public productionMeters: EDGE_CONFIG.COMPONENT[] = [];
    public chargerComponents: EDGE_CONFIG.COMPONENT[] = [];
    public isAsymmetric: boolean = false;

    protected override getChannelAddresses() {
        const channelAddresses: ChannelAddress[] = [];

        // Get Chargers
        THIS.CHARGER_COMPONENTS =
            THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")
                .filter(component => COMPONENT.IS_ENABLED);

        // Get productionMeters
        THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
            .filter(component => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_PRODUCER(component))
            .forEach(component => {
                CHANNEL_ADDRESSES.PUSH(new ChannelAddress(COMPONENT.ID, "ActivePower"));
                CHANNEL_ADDRESSES.PUSH(new ChannelAddress(COMPONENT.ID, "ActivePowerL1"));
                CHANNEL_ADDRESSES.PUSH(new ChannelAddress(COMPONENT.ID, "ActivePowerL2"));
                CHANNEL_ADDRESSES.PUSH(new ChannelAddress(COMPONENT.ID, "ActivePowerL3"));
                THIS.PRODUCTION_METERS.PUSH(component);
            });

        return channelAddresses;
    }
}
