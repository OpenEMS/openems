import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { TimeOfUseTariffUtils } from "src/app/shared/utils/utils";
import { environment } from "src/environments";
import { EvseChargepoint } from "./evse-chargepoint";

export namespace ControllerEvseSingleShared {

    export function getNavigationTree(edge: Edge, translate: TranslateService, componentId: EdgeConfig.Component["id"], config: EdgeConfig): ConstructorParameters<typeof NavigationTree> | null {
        const component = config.getComponentSafely(componentId);
        const baseMode: NavigationTree["mode"] = "label";

        if (component == null) {
            return null;
        }

        return new NavigationTree(
            componentId, { baseString: "evse/" + componentId }, { name: "oe-evcs", color: "success" }, Name.METER_ALIAS_OR_ID(component), baseMode, [
                ...(edge.roleIsAtLeast(Role.ADMIN)
                    ? [new NavigationTree("forecast", { baseString: "forecast" }, { name: "stats-chart-outline", color: "success" }, translate.instant("INSTALLATION.CONFIGURATION_EXECUTE.PROGNOSIS"), baseMode, [], null)]
                    : []),

                new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), baseMode, [], null),
                new NavigationTree("energy-limit", { baseString: "energy-limit" }, { name: "settings-outline", color: "medium" }, translate.instant("GENERAL.ENERGY_LIMIT"), baseMode, [], null),
                new NavigationTree("phase-switching", { baseString: "phase-switching" }, { name: "menu-outline", color: "warning" }, translate.instant("EDGE.INDEX.WIDGETS.EVCS.PHASE_SWITCHING"), "label", [], null, getPhaseSwitchingShowOrder(componentId, edge, config)),
                new NavigationTree("schedule", { baseString: "schedule" }, { name: "calendar-outline", color: "warning" }, translate.instant("EDGE.INDEX.WIDGETS.EVSE.SCHEDULE.SCHEDULE"), baseMode, [
                    new NavigationTree("edit-task", { baseString: "edit-task" }, { name: "create-outline" }, translate.instant("JS_SCHEDULE.EDIT_TASK"), "label", [], null, "HIDE"),
                    new NavigationTree("add-task", { baseString: "add-task" }, { name: "add-outline" }, translate.instant("JS_SCHEDULE.ADD_TASK"), "label", [], null, "HIDE"),
                ], null),
                new NavigationTree("charge-mode", { baseString: "charge-mode" }, { name: "checkmark-done-outline", color: "medium" }, translate.instant("EDGE.INDEX.WIDGETS.EVSE.CHARGE_MODE"), baseMode, [], null),
                ...(edge.roleIsAtLeast(Role.OWNER)
                    ? [new NavigationTree("car", { baseString: "car/update/App.Evse.ElectricVehicle.Generic" }, { name: "car-sport-outline", color: "success" }, translate.instant("EVSE_SINGLE.HOME.VEHICLES"), baseMode, [], null)]
                    : []),
            ], null).toConstructorParams();
    }

    /**
     * Gets the phase switching showOrder.
     *
     * @param componentId the component id
     * @param edge the current edge
     * @param config the edge config
     * @returns "HIDE" if phase-switching is disabled, else "LOW"
     */
    export function getPhaseSwitchingShowOrder(componentId: string, edge: Edge, config: EdgeConfig): NavigationTree["showOrder"] {

        if (edge == null || config == null) {
            return "HIDE";
        }

        const component = config.getComponentFromOtherComponentsProperty(componentId, "chargePoint.id") ?? null;

        if (component == null) {
            return "HIDE";
        }

        const chargePointComponent = EvseChargepoint.getEvseChargepoint(component);
        if (!chargePointComponent?.hasPhaseSwitchingAbility()) {
            return "HIDE";
        }

        return "LOW";
    }

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_MODE_LABEL = (translate: TranslateService) => {
        return (raw: string | null): string => {
            return Converter.IF_STRING(raw, value => {
                switch (value) {
                    case Mode.ZERO:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.ZERO");
                    case Mode.MINIMUM:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.MINIMUM");
                    case Mode.SURPLUS:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.SURPLUS");
                    case Mode.FORCE:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.FORCE");
                    default:
                        return Converter.HIDE_VALUE(value);
                }
            });
        };
    };

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_PHASE_SWITCH_LABEL = (translate: TranslateService) => {
        return (raw: string | null): string => {
            return Converter.IF_STRING(raw, value => {
                switch (value) {
                    case "DISABLE":
                        return translate.instant("EVSE_SINGLE.HOME.MODE.ZERO");
                    case "FORCE_SINGLE_PHASE":
                        return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.PHASE_SWITCH_TO_SINGLE_PHASE");
                    case "FORCE_THREE_PHASE":
                        return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.PHASE_SWITCH_TO_THREE_PHASE");
                    default:
                        return Converter.HIDE_VALUE(value);
                }
            });
        };
    };

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_ACTUAL_MODE_LABEL = (translate: TranslateService) => {
        return (raw: number | null): string => {
            return Converter.IF_NUMBER(raw, value => {
                switch (value) {
                    case 0:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.ZERO");
                    case 1:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.MINIMUM");
                    case 2:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.SURPLUS");
                    case 3:
                        return translate.instant("EVSE_SINGLE.HOME.MODE.FORCE");
                    default:
                        return Converter.HIDE_VALUE(value);
                }
            });
        };
    };

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_ENERGY_LIMIT_LABEL = () => {
        return (raw: number | null): string => {
            return Converter.IF_NUMBER(raw, value => {
                if (value <= 0) {
                    return Converter.HIDE_VALUE(value);
                }
                return value.toString();
            });
        };
    };

    /**
     * Converts a string mode to a presentable label
     *
     * @param raw the raw value
     * @returns the value for chosen mode
     */
    export const CONVERT_TO_STATE_MACHINE_LABEL = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case StateMachine.EV_NOT_CONNECTED:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.EV_NOT_CONNECTED");
                case StateMachine.EV_CONNECTED:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.EV_CONNECTED");
                case StateMachine.CHARGING:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.CHARGING");
                case StateMachine.FINISHED_EV_STOP:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.FINISHED_EV_STOP");
                case StateMachine.FINISHED_ENERGY_SESSION_LIMIT:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.FINISHED_ENERGY_SESSION_LIMIT");
                case StateMachine.PHASE_SWITCH_TO_THREE_PHASE:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.PHASE_SWITCH_TO_THREE_PHASE");
                case StateMachine.PHASE_SWITCH_TO_SINGLE_PHASE:
                    return translate.instant("EVSE_SINGLE.HOME.STATE_MACHINE.PHASE_SWITCH_TO_SINGLE_PHASE");
                default:
                    return "-";
            }
        };
    };

    export enum StateMachine {
        UNDEFINED = -1,
        EV_NOT_CONNECTED = 10,
        EV_CONNECTED = 20,
        CHARGING = 50,
        FINISHED_EV_STOP = 60,
        FINISHED_ENERGY_SESSION_LIMIT = 61,
        PHASE_SWITCH_TO_THREE_PHASE = 91,
        PHASE_SWITCH_TO_SINGLE_PHASE = 92,
    }

    export function getImgUrlByFactoryId(factoryId: string): string | null {
        switch (factoryId) {
            case "Evse.ChargePoint.Keba.UDP":
                return environment.images.EVSE.KEBA_P30;
            case "Evse.ChargePoint.Keba.Modbus":
                return environment.images.EVSE.KEBA_P40;
            case "Evse.ChargePoint.HardyBarth":
                return environment.images.EVSE.HARDY_BARTH;
            default:
                return null;
        }
    }

    export type ScheduleChartData = {
        datasets: ChartDataset[],
        colors: any[],
        labels: Date[]
    };

    export enum Mode {
        ZERO = "ZERO",
        MINIMUM = "MINIMUM",
        SURPLUS = "SURPLUS",
        FORCE = "FORCE",
    }

    /**
     * Gets the schedule chart data containing datasets, colors and labels.
     *
     * @param size The length of the dataset
     * @param prices The quarterly price array
     * @param modes The modes array
     * @param timestamps The timestamps array
     * @param translate The Translate service
     * @returns The ScheduleChartData.
     */
    export function getScheduleChartData(size: number, prices: number[], modes: number[], timestamps: string[],
        translate: TranslateService): ControllerEvseSingleShared.ScheduleChartData {

        const datasets: ChartDataset[] = [];
        const colors: any[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barZero = Array(size).fill(null);
        const barMinimum = Array(size).fill(null);
        const barSurplus = Array(size).fill(null);
        const barForce = Array(size).fill(null);

        for (let index = 0; index < size; index++) {
            const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(prices[index]);
            const mode = modes[index];
            labels.push(new Date(timestamps[index]));

            const modeStates = Object.keys(ControllerEvseSingleShared.Mode);

            if (mode !== null) {
                switch (mode) {
                    case modeStates.indexOf(ControllerEvseSingleShared.Mode.ZERO):
                        barZero[index] = quarterlyPrice;
                        break;
                    case modeStates.indexOf(ControllerEvseSingleShared.Mode.MINIMUM):
                        barMinimum[index] = quarterlyPrice;
                        break;
                    case modeStates.indexOf(ControllerEvseSingleShared.Mode.SURPLUS):
                        barSurplus[index] = quarterlyPrice;
                        break;
                    case modeStates.indexOf(ControllerEvseSingleShared.Mode.FORCE):
                        barForce[index] = quarterlyPrice;
                        break;
                }
            }
        }

        // Set datasets
        datasets.push({
            type: "bar",
            label: "No Charge",
            data: barZero,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(0,0,0,0.8)",
            borderColor: "rgba(0,0,0,0.9)",
        });

        datasets.push({
            type: "bar",
            label: "Minimum",
            data: barMinimum,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(25, 19, 82, 0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        datasets.push({
            type: "bar",
            label: "Surplus PV",
            data: barSurplus,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(51,102,0,0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        datasets.push({
            type: "bar",
            label: "Force Charge",
            data: barForce,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(0, 204, 204,0.5)",
            borderColor: "rgba(0, 204, 204,0.7)",
        });

        const scheduleChartData: ControllerEvseSingleShared.ScheduleChartData = {
            colors: colors,
            datasets: datasets,
            labels: labels,
        };

        return scheduleChartData;
    }
}
