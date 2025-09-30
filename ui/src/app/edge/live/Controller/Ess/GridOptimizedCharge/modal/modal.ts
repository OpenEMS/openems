// @ts-strict-ignore
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
    templateUrl: "./MODAL.HTML",
    changeDetection: CHANGE_DETECTION_STRATEGY.ON_PUSH,
    standalone: false,
})
export class ModalComponent extends AbstractModal {

    public channelCapacity: number;
    public isAtLeastAdmin: boolean = false;
    public refreshChart: boolean;

    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
    public readonly CONVERT_MINUTE_TO_TIME_OF_DAY = Utils.CONVERT_MINUTE_TO_TIME_OF_DAY(THIS.TRANSLATE);
    public readonly CONVERT_TO_WATTHOURS = Utils.CONVERT_TO_WATTHOURS;
    public readonly DelayChargeState = DelayChargeState;
    public state: string = "";
    public chargeLimit: { name: string, value: number };
    public delayChargeState: number | null = null;
    public maximumSellToGridPower: number | null = null;
    public targetMinute: number | null = null;
    public delayChargeMaximumChargeLimit: number | null = null;
    public targetEpochSeconds: number | null = null;
    public chargeStartEpochSeconds: number | null = null;

    protected override getChannelAddresses(): ChannelAddress[] {
        THIS.REFRESH_CHART = false;
        const channels: ChannelAddress[] = [];
        if (THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN)) {
            THIS.IS_AT_LEAST_ADMIN = true;
            if ("ESS.ID" in THIS.COMPONENT.PROPERTIES) {
                CHANNELS.PUSH(
                    new ChannelAddress(THIS.COMPONENT.PROPERTIES["ESS.ID"], "Capacity"),
                );
            }
        }
        CHANNELS.PUSH(
            new ChannelAddress(THIS.COMPONENT.ID, "SellToGridLimitState"),
            new ChannelAddress(THIS.COMPONENT.ID, "DelayChargeState"),
            new ChannelAddress(THIS.COMPONENT.ID, "SellToGridLimitMinimumChargeLimit"),
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertyMaximumSellToGridPower"),
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertySellToGridLimitEnabled"),
            new ChannelAddress(THIS.COMPONENT.ID, "TargetEpochSeconds"),
            new ChannelAddress(THIS.COMPONENT.ID, "TargetMinute"),
            new ChannelAddress(THIS.COMPONENT.ID, "DelayChargeMaximumChargeLimit"),
            new ChannelAddress(THIS.COMPONENT.ID, "PredictedChargeStartEpochSeconds"),
        );
        return channels;
    }

    protected override onCurrentData(currentData: CurrentData) {

        // If the gridfeed in Limit is avoided
        if (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitState"] == SellToGridLimitState.ACTIVE_LIMIT_FIXED ||
            (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitState"] == SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT &&
                CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeState"] != DelayChargeState.ACTIVE_LIMIT &&
                CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitMinimumChargeLimit"] > 0)) {
            THIS.CHARGE_LIMIT = {
                name: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MINIMUM_CHARGE"),
                value: CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SellToGridLimitMinimumChargeLimit"],
            };
            THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.GRID_FEED_IN_LIMITATION_IS_AVOIDED");

        } else {

            // DelayCharge State
            switch (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeState"]) {
                case -1: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NOT_DEFINED");
                    break;
                case 0: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.CHARGE_LIMIT_ACTIVE");
                    break;
                case 1: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.PASSED_END_TIME");
                    break;
                case 2: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.STORAGE_ALREADY_FULL");
                    break;
                case 3: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.END_TIME_NOT_CALCULATED");
                    break;
                case 4: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_POSSIBLE");
                    break;
                case 5: // Case 6: 'DISABLED' hides 'state-line', so no Message needed
                case 7: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_ACTIVE");
                    break;

                case 8: THIS.STATE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.CHARGING_DELAYED");
                    break;
            }

            // DelayCharge Maximum Charge Limit
            if (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeMaximumChargeLimit"] != null) {
                THIS.CHARGE_LIMIT = {
                    name: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_CHARGE"),
                    value: CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeMaximumChargeLimit"],
                };
            }
        }
        THIS.DELAY_CHARGE_STATE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeState"];

        // Capacity (visible for admin only)
        if (THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN) && "ESS.ID" in THIS.COMPONENT.PROPERTIES) {
            THIS.CHANNEL_CAPACITY = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.PROPERTIES["ESS.ID"] + "/Capacity"];
        }

        THIS.MAXIMUM_SELL_TO_GRID_POWER = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyMaximumSellToGridPower"];
        THIS.TARGET_MINUTE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/TargetMinute"];
        THIS.DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/DelayChargeMaximumChargeLimit"];
        THIS.TARGET_EPOCH_SECONDS = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/TargetEpochSeconds"];
        THIS.CHARGE_START_EPOCH_SECONDS = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/PredictedChargeStartEpochSeconds"];
    }

    protected override getFormGroup(): FormGroup {
        return THIS.FORM_BUILDER.GROUP({
            mode: new FormControl(THIS.COMPONENT.PROPERTIES.MODE),
            sellToGridLimitEnabled: new FormControl(THIS.COMPONENT.PROPERTIES.SELL_TO_GRID_LIMIT_ENABLED),
            maximumSellToGridPower: new FormControl(THIS.COMPONENT.PROPERTIES.MAXIMUM_SELL_TO_GRID_POWER, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
                VALIDATORS.REQUIRED,
            ])),
            delayChargeRiskLevel: new FormControl(THIS.COMPONENT.PROPERTIES.DELAY_CHARGE_RISK_LEVEL),
            manualTargetTime: new FormControl(THIS.COMPONENT.PROPERTIES.MANUAL_TARGET_TIME),
        });
    }
}

export enum DelayChargeState {
    UNDEFINED = -1, // Undefined
    ACTIVE_LIMIT = 0, // Active limit
    NO_REMAINING_TIME = 1, // No remaining time
    NO_REMAINING_CAPACITY = 2, // No remaining capacity //
    TARGET_MINUTE_NOT_CALCULATED = 3, // Target minute not calculated //
    NO_FEASIBLE_SOLUTION = 4, // Limit cannot be adapted because of other constraints with higher priority
    NO_CHARGE_LIMIT = 5, // No active limitation
    DISABLED = 6, // Delay charge part is disabled
    NOT_STARTED = 7, // Delay charge was not started because there is no production or to less production
    AVOID_LOW_CHARGING = 8, // Avoid charging with low power for more efficiency
}

export enum SellToGridLimitState {
    UNDEFINED = -1,// Undefined
    ACTIVE_LIMIT_FIXED = 0,// Active limitation - Fix limit
    NO_LIMIT = 1,//No active limitation
    NO_FEASIBLE_SOLUTION = 2,//Limit cannot be adapted because of other constraints with higher priority
    ACTIVE_LIMIT_CONSTRAINT = 3,// Active limitation - Minimum charge power
    DISABLED = 4, // SellToGridLimit part is disabled
    NOT_STARTED = 5,//SellToGridLimit part was not started because there is no production or to less production
}
