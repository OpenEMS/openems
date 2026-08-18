// @ts-strict-ignore
export { ChartConstants } from "./components/chart/chart.constants";
export { Edge } from "./components/edge/edge";
export { EdgeConfig } from "./components/edge/edgeconfig";
export { Logger } from "./service/logger";
export { Service } from "./service/service";
export { Websocket } from "./service/websocket";
export { ChannelAddress } from "./type/channeladdress";
export { CurrentData } from "./type/currentdata";
export { GridMode } from "./type/general";
export { SystemLog } from "./type/systemlog";
export { Utils } from "./utils/utils";
import { AlertController, AlertOptions } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { isBefore, subDays, subYears } from "date-fns";

import { addIcons } from "ionicons";
import { environment } from "src/environments";
import { Edge } from "./components/edge/edge";
import { User } from "./jsonrpc/shared";
import { DefaultTypes } from "./type/defaulttypes";
import { Role } from "./type/role";
import { StringUtils } from "./utils/string/string.utils";

addIcons({
    "oe-consumption": environment.icons.COMMON.CONSUMPTION,
    "oe-heatpump": environment.icons.COMPONENT.HEATPUMP,
    "oe-heating-element": environment.icons.COMPONENT.HEATING_ELEMENT,
    "oe-selfconsumption": environment.icons.COMMON.SELFCONSUMPTION,
    "oe-evcs": environment.icons.COMPONENT.EVCS,
    "oe-grid": environment.icons.COMMON.GRID,
    "oe-grid-storage": environment.icons.COMMON.GRID_STORAGE,
    "oe-grid-restriction": environment.icons.COMMON.GRID_RESTRICTION,
    "oe-megafon": environment.icons.COMMON.MEGAFON,
    "oe-offgrid": environment.icons.COMMON.OFFGRID,
    "oe-production": environment.icons.COMMON.PRODUCTION,
    "oe-storage": environment.icons.COMMON.STORAGE,
    "oe-checkmark": environment.icons.STATUS.CHECKMARK,
    "oe-error": environment.icons.STATUS.ERROR,
    "oe-warning": environment.icons.STATUS.WARNING,
    "oe-info": environment.icons.STATUS.INFO,
    "oe-offline": environment.icons.COMMON.OFFLINE.CLOUD_OFFLINE_OUTLINE,
    "oe-time-of-use": environment.icons.COMMON.TIME_OF_USE.TIME_OF_USE,
    "oe-time-of-use-thin": environment.icons.COMMON.TIME_OF_USE.TIME_OF_USE_THIN,
    "oe-generator": environment.icons.COMMON.GENERATOR,
    "oe-energy-journey": environment.icons.ENERGY_JOURNEY,
    "oe-battery-extension": environment.icons.BATTERY_EXTENSION,
    "oe-wrap-up": environment.icons.WRAP_UP,
    "oe-favorites": environment.icons.COMMON.FAVORITES,
});

export class Permission {}

export class EdgePermission {
    /**
     * Checks if the edge has the switchArchitecture jsonRpc logic.
     *
     * @param edge The edge to check
     * @returns True if the edge has the switchArchitecture jsonRpc logic, false otherwise
     */
    public static hasSwitchArchitecture(edge: Edge): boolean {
        return edge.isVersionAtLeast("2025.12.4");
    }

    /**
     * Checks if user is allowed to see {@link ProfileComponent} setup protocol download
     *
     * @param edge The edge
     * @returns True, if user is at least {@link Role.OWNER}
     */
    public static isUserAllowedToSetupProtocolDownload(edge: Edge): boolean {
        return Role.isAtLeast(edge.role, Role.OWNER);
    }

    /**
     * Checks if the {@link EnergyJourneyComponent energy journey} is allowed to be seen
     *
     * @param ibnDate The ibn date - first setup protocol date
     * @returns True, if ibnDate is at least one year ago and edge producttype is 'Home 10'
     */
    public static isEnergyJourneyAllowed(edge: Edge): boolean {
        const isDateAtLeastOneYearAgo = isBefore(edge.firstSetupProtocol, subDays(subYears(new Date(), 1), 1));
        return (
            isDateAtLeastOneYearAgo && StringUtils.isInArr(edge.producttype, [])
        );
    }

    /**
     * Gets the allowed history periods for this edge, used in {@link PickDatePopoverComponent} and if histroyPeriods
     * exist, it gets the correspondent periods accordingly
     *
     * @param edge The edge
     * @param historyPeriods The historyPeriods i.e 'day', 'week' or 'custom'
     * @returns The list of allowed periods for this edge
     */
    public static getAllowedHistoryPeriods(edge: Edge, historyPeriods?: DefaultTypes.PeriodStringValues[]) {
        if (historyPeriods?.length > 0) {
            return historyPeriods;
        }

        return Object.values(DefaultTypes.PeriodString).reduce((arr, el) => {
            // hide total, if no first ibn date
            if (el === DefaultTypes.PeriodString.TOTAL && edge?.firstSetupProtocol === null) {
                return arr;
            }

            arr.push(el);
            return arr;
        }, []);
    }

    /**
     * Checks if the edge version is at least 2025.12.1 to cover systemErrorAcknowledge JSON-RPC request.
     *
     * @param edge The edge to check
     * @returns True if the edge is 2025.12.1
     */
    public static hasSystemErrorAcknowledge(edge: Edge): boolean {
        return edge.isVersionAtLeast("2025.12.1");
    }

    /**
     * Checks if the edge version is at least 2026.8.2 to and temporally if the user is ADMIN to access the time
     * schedule and base mode in the heatpump component.
     *
     * @param edge The edge to check
     * @returns True if the edge is 2026.8.2
     */
    public static isHeatpumpTimeScheduleAndBaseModeAvailable(edge: Edge): boolean {
        return edge.isVersionAtLeast("2026.8.2") && edge.roleIsAtLeast(Role.ADMIN);
    }
}

export class UserPermission {
    /**
     * Checks if user is allowed to see {@link FooterComponent}
     *
     * @param user The current user
     * @returns True, if user is at least {@link Role.GUEST}
     */
    public static isUserAllowedToSeeFooter(user: User): boolean {
        return Role.isAtLeast(user.globalRole, Role.GUEST);
    }

    /**
     * Checks if user is allowed to see the Overview page.
     *
     * @param user The current user
     * @returns True, if user is allowed to see the overview page
     */
    public static isUserAllowedToSeeOverview(user: User): boolean {
        if (environment.backend === "OpenEMS Edge") {
            return false;
        }
        if (user.hasMultipleEdges) {
            return true;
        }
        if (Role.isAtLeast(user.globalRole, Role.INSTALLER)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if user is allowed to see {@link SystemRestartComponent}
     *
     * @param user The current user
     * @returns True, if user is at least {@link Role.ADMIN} and edge version is at least 2024.2.2
     */
    public static isAllowedToSeeSystemRestart(user: User, edge: Edge) {
        return Role.isAtLeast(user?.globalRole, Role.OWNER);
    }

    /**
     * Checks if user is allowed to see additional updates.
     *
     * @param edge The current {@link Edge}
     * @returns True, if user has access to see additional updates
     */
    public static isAllowedToSeeAdditionalUpdates(edge: Edge) {
        return edge.isVersionAtLeast("2025.5.4") && edge.roleIsAtLeast(Role.ADMIN);
    }
}

export enum Producttype {}

export namespace Currency {
    /**
     * This method returns the corresponding label based on the user-selected currency in "core.meta."
     *
     * @param currency The currency enum.
     * @returns The Currencylabel
     */
    export function getCurrencyLabelByCurrency(currency: string | null): Label {
        switch (currency) {
            case "SEK":
                return Label.OERE_PER_KWH;
            case "CHF":
                return Label.RAPPEN_PER_KWH;
            case null:
            default:
                return Label.CENT_PER_KWH;
        }
    }

    /**
     * This method returns the corresponding label for the chart based on the user-selected currency.
     *
     * @param currency The currency enum.
     * @returns The Currency Unit label
     */
    export function getChartCurrencyUnitLabel(currency: string) {
        switch (currency) {
            case "SEK":
                return Unit.OERE;
            case "CHF":
                return Unit.RAPPEN;
            default:
                return Unit.CENT;
        }
    }

    export enum Label {
        OERE_PER_KWH = "Öre/kWh",
        CENT_PER_KWH = "ct/kWh",
        RAPPEN_PER_KWH = "Rp./kWh",
    }

    export enum Unit {
        CENT = "Cent",
        OERE = "Öre",
        RAPPEN = "Rp.",
    }
}

export enum ChannelRegister {
    SetActivePowerEquals = 706,
    SetReactivePowerEquals = 708,
    SetActivePowerLessOrEquals = 710,
    SetReactivePowerLessOrEquals = 712,
    SetActivePowerGreaterOrEquals = 714,
    SetReactivePowerGreaterOrEquals = 716,
}

export enum RippleControlReceiverRestrictionLevel {
    NO_RESTRICTION = 0,
    ZERO_PERCENT = 1,
    THIRTY_PERCENT = 2,
    SIXTY_PERCENT = 3,
}

export enum Limiter14aRestriction {
    NO_RESTRICTION = 0,
    RESTRICTION = 1,
}

/** Presents a simple */
export async function presentAlert(
    alertController: AlertController,
    translate: TranslateService,
    alertOptions: AlertOptions,
) {
    if (!alertOptions?.buttons) {
        throw new Error("Confirmation button is missing");
    }

    const alert = alertController.create({
        ...alertOptions,
        buttons: [
            {
                text: translate.instant("GENERAL.CANCEL"),
                role: "cancel",
            },
            ...(alertOptions?.buttons ?? []),
        ],
        cssClass: "alertController",
    });
    (await alert).present();
}
