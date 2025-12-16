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
import { addIcons } from "ionicons";
import { environment } from "src/environments";
import { Edge } from "./components/edge/edge";
import { User } from "./jsonrpc/shared";
import { DefaultTypes } from "./type/defaulttypes";
import { Role } from "./type/role";

addIcons({
    "oe-consumption": environment.icons.COMMON.CONSUMPTION,
    "oe-heatpump": environment.icons.COMPONENT.HEATPUMP,
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
});

export class Permission {
}

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
     * @param edge the edge
     * @returns true, if user is at least {@link Role.OWNER}
     */
    public static isUserAllowedToSetupProtocolDownload(edge: Edge): boolean {
        return Role.isAtLeast(edge.role, Role.OWNER);
    }

    /**
   * Gets the allowed history periods for this edge, used in {@link PickDatePopoverComponent}
   * and if histroyPeriods exist, it gets the correspondent periods accordingly
   *
   * @param edge the edge
   * @param historyPeriods the historyPeriods i.e 'day', 'week' or 'custom'
   * @returns the list of allowed periods for this edge
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

    public static isModbusTcpApiWidgetAllowed(edge: Edge): boolean {
        return edge?.isVersionAtLeast("2024.9.1");
    }

    /**
   * Determines if the edge has its channels in the edgeconfig
   * or if they should be obtained with a separate request.
   *
   * The reason this was introduced is to reduce the size of the EdgeConfig
   * and therefore improve performance in network, backend, ui, edge.
   *
   * @returns true if the channels are included in the edgeconfig
   */
    public static hasChannelsInEdgeConfig(edge: Edge): boolean {
        return !edge.isVersionAtLeast("2024.6.1");
    }

    /**
   * Determines if the edge has only the factories which are used by the
   * active components in the edgeconfig or if all factories are inlcuded.
   *
   * The reason this was introduced is to reduce the size of the EdgeConfig
   * and therefore improve performance in network, backend, ui, edge.
   *
   * @returns true if only the factories of the used components are in the edgeconfig
   */
    public static hasReducedFactories(edge: Edge): boolean {
        return edge.isVersionAtLeast("2024.6.1");
    }
}

export class UserPermission {

    /**
   * Checks if user is allowed to see  {@link FooterComponent}
   *
   * @param user the current user
   * @returns true, if user is at least {@link Role.GUEST}
   */
    public static isUserAllowedToSeeFooter(user: User): boolean {
        return Role.isAtLeast(user.globalRole, Role.GUEST);
    }

    public static isUserAllowedToSeeOverview(user: User): boolean {

        if (Role.isAtLeast(user.globalRole, Role.INSTALLER)) {
            return true;
        }

        return user.hasMultipleEdges;
    }


    /**
  * Checks if user is allowed to see {@link SystemRestartComponent}
  *
  * @param user the current user
  * @returns true, if user is at least {@link Role.ADMIN} and edge version is at least 2024.2.2
  */
    public static isAllowedToSeeSystemRestart(user: User, edge: Edge) {
        const isAllowed = edge?.isVersionAtLeast("2024.2.2");
        return Role.isAtLeast(user?.globalRole, Role.OWNER) && isAllowed;
    }

    /**
  * Checks if user is allowed to see additional updates.
  *
  * @param edge the current {@link Edge}
  * @returns true, if user has access to see additional updates
  */
    public static isAllowedToSeeAdditionalUpdates(edge: Edge) {
        return edge.isVersionAtLeast("2025.5.4") && edge.roleIsAtLeast(Role.ADMIN);
    }

}

export enum Producttype {
}

export namespace Currency {

    /**
   * This method returns the corresponding label based on the user-selected currency in "core.meta."
   *
   * @param currency The currency enum.
   * @returns the Currencylabel
   */
    export function getCurrencyLabelByCurrency(currency: string): Label {
        switch (currency) {
            case "SEK":
                return Label.OERE_PER_KWH;
            case "CHF":
                return Label.RAPPEN_PER_KWH;
            default:
                return Label.CENT_PER_KWH;
        }
    }

    /**
   * This method returns the corresponding label for the chart based on the user-selected currency.
   *
   * @param currency The currency enum.
   * @returns the Currency Unit label
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
        CENT_PER_KWH = "Cent/kWh",
        RAPPEN_PER_KWH = "Rp./kWh",
    }

    export enum Unit {
        CENT = "Cent",
        OERE = "Öre",
        RAPPEN = "Rp.",
    }
}

export enum ChannelRegister {
    "SetActivePowerEquals" = 706,
    "SetReactivePowerEquals" = 708,
    "SetActivePowerLessOrEquals" = 710,
    "SetReactivePowerLessOrEquals" = 712,
    "SetActivePowerGreaterOrEquals" = 714,
    "SetReactivePowerGreaterOrEquals" = 716,
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

/**
* Presents a simple
*/
export async function presentAlert(alertController: AlertController, translate: TranslateService, alertOptions: AlertOptions) {

    if (!alertOptions?.buttons) {
        throw new Error("Confirmation button is missing");
    }

    const alert = alertController.create({
        ...alertOptions,
        buttons: [{
            text: translate.instant("GENERAL.CANCEL"),
            role: "cancel",
        },
        ...(alertOptions?.buttons ?? []),
        ],
        cssClass: "alertController",
    });
    (await alert).present();
}
