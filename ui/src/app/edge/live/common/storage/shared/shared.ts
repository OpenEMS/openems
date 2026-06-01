import { TranslateService } from "@ngx-translate/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Filter } from "src/app/shared/components/shared/filter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { SharedEssFixDigitalPowerControl } from "../../../Controller/Ess/FixActivePower/shared/shared";
import { SharedGridOptimizedCharge } from "../../../Controller/Ess/GridOptimizedCharge/shared/shared";
import { SharedControllerEssTimeOfUseTariff } from "../../../Controller/Ess/TimeOfUseTariff/shared/shared";

export namespace SharedStorage {

    export function getNavigationTree(edge: Edge, translate: TranslateService, config: EdgeConfig) {
        const essFixActivePowerController = config.getComponentsByFactory("Controller.Ess.FixActivePower");
        const essGridOptimizedChargeController = config.getComponentsByFactory("Controller.Ess.GridOptimizedCharge");
        const timeOfUseTariffController = config.getComponentsByFactory("Controller.Ess.Time-Of-Use-Tariff");


        const essController: NavigationTree[] = [
            ...timeOfUseTariffController
                .filter(component => component.isEnabled)
                .map(component => new NavigationTree(
                    ...SharedControllerEssTimeOfUseTariff.getNavigationTree(translate, component)
                )),

            ...essGridOptimizedChargeController
                .filter(component => component.isEnabled)
                .map(component => new NavigationTree(
                    ...SharedGridOptimizedCharge.getNavigationTree(translate, component)
                )),

            ...essFixActivePowerController
                .filter(component => component.isEnabled)
                .map(component => new NavigationTree(
                    ...SharedEssFixDigitalPowerControl.getNavigationTree(translate, component)
                )),
        ];

        const essComponents =
            config?.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
                .filter(component => component.isEnabled && !component.factoryId.includes("Ess.Cluster"));

        const historyChildren = essComponents.length <= 1 ? [] : essComponents.map(el => new NavigationTree(el.id, { baseString: el.id + "/phase-accurate" }, { color: "success", name: "stats-chart-outline" }, el.alias, "label", [], null));

        const emergencyReserveCtrl = config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve");
        const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension");
        const hasAtLeastOneController = emergencyReserveCtrl.length > 0 || prepareBatteryExtensionCtrl.length > 0;

        return new NavigationTree("storage", { baseString: "common/storage" }, { name: "oe-storage", color: "success" }, translate.instant("GENERAL.STORAGE_SYSTEM"), "label", [
            ...essController,
            NavigationConstants.CommonNodes.PHASE_ACCURATE(translate, "details", "success"),
            NavigationConstants.CommonNodes.HISTORY(translate, historyChildren),
            NavigationConstants.CommonNodes.SETTINGS(translate, hasAtLeastOneController ? "LOW" : "HIDE"),
        ], null).toConstructorParams();
    }

    export const convertToPower: Converter = (value: number | string | null): string => {
        return Converter.IF_NUMBER(value, (val) => {
            if (val > 0) {
                return convertDischargePhasePower(val);
            }
            return convertChargePhasePower(val);
        });
    };

    export const convertChargePowerInW: Converter = (raw: number | string | null): string => {
        return Converter.IF_NUMBER(raw, (value) => {
            return powerInW(NumberUtils.multiplySafely(value, -1), 1, true);
        });
    };

    export const convertChargePhasePower: Converter = (raw: number | string | null): string => {
        return Converter.IF_NUMBER(raw, (value) => {
            return powerInW(NumberUtils.multiplySafely(value, -1), 3, true);
        });
    };

    export const convertDischargePhasePower: Converter = (raw: number | string | null): string => {
        return Converter.IF_NUMBER(raw, (value) => {
            return powerInW(value, 3, true);
        });
    };

    export const convertChargePowerInKw: Converter = (raw: number | string | null): string => {
        return Converter.IF_NUMBER(raw, (value) => {
            return powerInKw(NumberUtils.multiplySafely(value, -1), 1, true);
        });
    };

    export function powerInKw(value: number | null, divisor: number = 1, isCharge?: boolean) {
        if (value == null) {
            return "-";
        }

        const thisValue: number | null = NumberUtils.divideSafely(NumberUtils.divideSafely(value, 1000), divisor);

        // Round thisValue to Integer when decimal place equals 0
        if (thisValue != null && thisValue > 0) {
            return Formatter.FORMAT_KILO_WATT(thisValue);

        } else if (thisValue == 0 && isCharge) {
            // if thisValue is 0, then show only when charge and not discharge
            return Formatter.FORMAT_KILO_WATT(0);

        } else {
            return "-";
        }
    }

    export function powerInW(value: number | null, divisor: number = 1, isCharge?: boolean) {
        if (value == null) {
            return "-";
        }
        const thisValue = NumberUtils.divideSafely(value, divisor);

        // Round value to Integer when decimal place equals 0
        if (thisValue != null && thisValue > 0) {
            return Formatter.FORMAT_WATT(thisValue);

        } else if (thisValue == 0 && isCharge) {
            // if value is 0, then show only when charge and not discharge
            return Formatter.FORMAT_WATT(0);

        } else {
            return "-";
        }
    }

    export function getBatteryCapacityExtensionStatus(translate: TranslateService, currentData: CurrentData, controllerId: EdgeConfig.Component["id"]): { color: string, text: string } | null {
        const isRunning: boolean = currentData.allComponents[controllerId + "/_PropertyIsRunning"] == 1;
        const essIsBlocking: number = currentData.allComponents[controllerId + "/CtrlIsBlockingEss"];
        const essIsCharging: number = currentData.allComponents[controllerId + "/CtrlIsChargingEss"];
        const essIsDischarging: number = currentData.allComponents[controllerId + "/CtrlIsDischargingEss"];
        const targetTimeSpecified: boolean = currentData.allComponents[controllerId + "/_PropertyTargetTimeSpecified"];
        const targetDate: Date = currentData.allComponents[controllerId + "/_PropertyTargetTime"];
        const isInReferenceCycle: boolean = currentData.allComponents[controllerId + "/CtrlIsInReferenceCycle"] === 1;

        if (!isRunning) {
            return null;
        }

        if (isInReferenceCycle) {
            return { color: "orange", text: translate.instant("EDGE.INDEX.RETROFITTING.PREPARING") };
        }

        // Planned Expansion
        if (targetTimeSpecified != null && targetDate != null) {

            return {
                color: "green", text: translate.instant("EDGE.INDEX.RETROFITTING.TARGET_TIME_SPECIFIED", {
                    targetDate: DateUtils.toLocaleDateString(targetDate),
                    targetTime: targetDate.toLocaleTimeString(),
                }),
            };
        }

        if (essIsBlocking != null && essIsBlocking == 1) {
            // If ess reached targetSoc
            return { color: "green", text: translate.instant("EDGE.INDEX.RETROFITTING.REACHED_TARGET_SOC") };

        } else if ((essIsCharging != null && essIsCharging == 1) || (essIsDischarging != null && essIsDischarging == 1)) {

            // If Ess is charging to or discharging to the targetSoc
            return { color: "orange", text: translate.instant("EDGE.INDEX.RETROFITTING.PREPARING") };
        } else {
            return null;
        }
    }

    export function getChargeDischargeLinesInKw(ess: EdgeConfig.Component, config: EdgeConfig, translate: TranslateService): OeFormlyField[] {
        const isHybridEss: boolean = config
            .getNatureIdsByFactoryId(ess.factoryId)
            .includes("io.openems.edge.ess.api.HybridEss");

        const channelId: ChannelAddress["channelId"] = isHybridEss ? "DcDischargePower" : "ActivePower";
        return [{
            type: "channel-line",
            channel: new ChannelAddress(ess.id, channelId).toString(),
            name: translate.instant("GENERAL.CHARGE"),
            converter: (value) => SharedStorage.convertChargePowerInKw(value),
        },
        {
            type: "channel-line",
            channel: new ChannelAddress(ess.id, channelId).toString(),
            name: translate.instant("GENERAL.DISCHARGE"),
            converter: (value) => SharedStorage.powerInKw(value),
        }];
    }

    export function getLinesPerEss(ess: EdgeConfig.Component, config: EdgeConfig, translate: TranslateService): OeFormlyField[] {

        const isHybridEss: boolean = config
            .getNatureIdsByFactoryId(ess.factoryId)
            .includes("io.openems.edge.ess.api.HybridEss");

        const channelId: ChannelAddress["channelId"] = isHybridEss ? "DcDischargePower" : "ActivePower";
        return [
            {
                type: "channel-line",
                channel: new ChannelAddress(ess.id, "Soc").toString(),
                name: translate.instant("GENERAL.SOC"),
                converter: Converter.STATE_IN_PERCENT,
                indentation: TextIndentation.SINGLE,
            },
            {
                type: "channel-line",
                channel: new ChannelAddress(ess.id, channelId).toString(),
                name: translate.instant("GENERAL.CHARGE"),
                converter: (value) => SharedStorage.convertChargePowerInW(value),
                filter: Filter.NOT_NULL_OR_UNDEFINED,
                indentation: TextIndentation.SINGLE,
            },
            {
                type: "channel-line",
                channel: new ChannelAddress(ess.id, channelId).toString(),
                name: translate.instant("GENERAL.DISCHARGE"),
                converter: (value) => SharedStorage.powerInW(value),
                filter: Filter.NOT_NULL_OR_UNDEFINED,
                indentation: TextIndentation.SINGLE,
            },
            ...getPhasesLines(translate, ess, channelId),
        ];
    }

    function getPhasesLines(translate: TranslateService, ess: EdgeConfig.Component, channelId: ChannelAddress["channelId"]): OeFormlyField[] {
        return Phase.THREE_PHASE.map(phase => ({
            type: "channel-line",
            channel: new ChannelAddress(ess.id, channelId).toString(),
            name: Name.SUFFIX_FOR_ESS_CHARGE_OR_DISCHARGE(translate, translate.instant("GENERAL.PHASE") + " " + phase),
            converter: (value) => SharedStorage.convertToPower(value),
            filter: () => true,
            indentation: TextIndentation.DOUBLE,
        }));
    }

    function getTotalPhasesLines(translate: TranslateService): OeFormlyField[] {
        return Phase.THREE_PHASE.map(phase => ({
            type: "channel-line",
            channel: new ChannelAddress("_sum", "EssActivePower").toString(),
            name: Name.SUFFIX_FOR_ESS_CHARGE_OR_DISCHARGE(translate, translate.instant("GENERAL.PHASE") + " " + phase),
            converter: (value) => SharedStorage.convertToPower(value),
            filter: () => true,
            indentation: TextIndentation.DOUBLE,
        }));
    }

    export function getTotalLines(translate: TranslateService, essComponents: EdgeConfig.Component[]): OeFormlyField[] {
        return [
            {
                type: "name-line", name: translate.instant("GENERAL.TOTAL"),
            },
            {
                type: "channel-line",
                channel: new ChannelAddress("_sum", "EssSoc").toString(),
                name: translate.instant("GENERAL.SOC"),
                converter: Converter.STATE_IN_PERCENT,
                indentation: TextIndentation.SINGLE,
            },
            {
                type: "channel-line",
                channel: new ChannelAddress("_sum", "EssActivePower").toString(),
                name: translate.instant("GENERAL.CHARGE"),
                converter: (value) => SharedStorage.convertChargePowerInW(value),
                filter: Filter.NOT_NULL_OR_UNDEFINED,
                indentation: TextIndentation.SINGLE,
            },
            {
                type: "channel-line",
                channel: new ChannelAddress("_sum", "EssActivePower").toString(),
                name: translate.instant("GENERAL.DISCHARGE"),
                converter: (value) => SharedStorage.powerInW(value),
                filter: Filter.NOT_NULL_OR_UNDEFINED,
                indentation: TextIndentation.SINGLE,
            },
            ...getTotalPhasesLines(translate),
            ...(essComponents.length > 1
                ? [{ type: "horizontal-line" }]
                : []) as OeFormlyField[],
        ];
    }

    export function getEssLines(translate: TranslateService, essComponents: EdgeConfig.Component[], config: EdgeConfig): OeFormlyField[] {
        if (essComponents.length <= 1) {
            return [];
        }
        return essComponents.reduce((arr: OeFormlyField[], ess, index) => {
            if (essComponents.length > 1) {
                arr.push({ type: "name-line", name: Name.METER_ALIAS_OR_ID(ess) });
            }

            arr.push(...SharedStorage.getLinesPerEss(ess, config, translate));

            if (index === essComponents.length - 1) {
                return arr;
            }
            arr.push({ type: "horizontal-line" });
            return arr;
        }, []);
    }

    export function getEssComponents(config: EdgeConfig) {
        return config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => {

                return (component.isEnabled || component.getPropertyFromComponent<boolean>("enabled") == true) && !config
                    .getNatureIdsByFactoryId(component.factoryId)
                    .includes("io.openems.edge.ess.api.MetaEss");
            });
    }
}
