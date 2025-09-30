// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Controller_Ess_GridOptimizedCharge",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    public override component: EDGE_CONFIG.COMPONENT | null = null;
    public mode: string = "-";
    public state: string = "-";
    public isSellToGridLimitAvoided: boolean = false;
    public sellToGridLimitMinimumChargeLimit: boolean = false;
    public delayChargeMaximumChargeLimit: number | null = null;
    public readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(THIS.TRANSLATE);
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: ModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {
        return [
            new ChannelAddress(THIS.COMPONENT_ID, "DelayChargeState"),
            new ChannelAddress(THIS.COMPONENT_ID, "SellToGridLimitState"),
            new ChannelAddress(THIS.COMPONENT_ID, "DelayChargeMaximumChargeLimit"),
            new ChannelAddress(THIS.COMPONENT_ID, "SellToGridLimitMinimumChargeLimit"),
            new ChannelAddress(THIS.COMPONENT_ID, "_PropertyMode"),
        ];
    }
    protected override onCurrentData(currentData: CurrentData) {
        THIS.MODE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyMode"];

        // Check if Grid feed in limitation is avoided
        if (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitState"] == 0 ||
            (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitState"] == 3
                && CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeState"] != 0
                && CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitMinimumChargeLimit"] > 0)) {
            THIS.IS_SELL_TO_GRID_LIMIT_AVOIDED = true;
        }

        THIS.SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitMinimumChargeLimit"];

        switch (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeState"]) {
            case -1:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NOT_DEFINED");
                break;
            case 0:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.CHARGE_LIMIT_ACTIVE");
                break;
            case 1:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.PASSED_END_TIME");
                break;
            case 2:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.STORAGE_ALREADY_FULL");
                break;
            case 3:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.END_TIME_NOT_CALCULATED");
                break;
            case 4:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_POSSIBLE");
                break;
            case 5:
            case 7:
                THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_ACTIVE");
                break;
            case 8: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.CHARGING_DELAYED");
                break;
        }

        THIS.DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeMaximumChargeLimit"];
    }

}
