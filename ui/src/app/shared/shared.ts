// @ts-strict-ignore
export { Edge } from "./components/edge/edge";
export { EdgeConfig } from "./components/edge/edgeconfig";
export { Logger } from "./service/logger";
export { Service } from "./service/service";
export { Utils } from "./service/utils";
export { Websocket } from "./service/websocket";
export { ChannelAddress } from "./type/channeladdress";
export { CurrentData } from "./type/currentdata";
export { GridMode } from "./type/general";
export { SystemLog } from "./type/systemlog";
export { Widget, WidgetFactory, WidgetNature, Widgets } from "./type/widget";

import { AlertController, AlertOptions } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { addIcons } from "ionicons";
import { Edge } from "./components/edge/edge";
import { User } from "./jsonrpc/shared";
import { DefaultTypes } from "./service/defaulttypes";
import { Role } from "./type/role";

addIcons({
  "oe-consumption": "assets/img/icon/consumption.svg",
  "oe-evcs": "assets/img/icon/evcs.svg",
  "oe-grid": "assets/img/icon/grid.svg",
  "oe-grid-storage": "assets/img/icon/gridStorage.svg",
  "oe-grid-restriction": "assets/img/icon/gridRestriction.svg",
  "oe-offgrid": "assets/img/icon/offgrid.svg",
  "oe-production": "assets/img/icon/production.svg",
  "oe-storage": "assets/img/icon/storage.svg",
});

export class EdgePermission {

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
}

export namespace Currency {

  /**
   * Gets the currencylabel for a edgeId
   *
   * @param edgeId the edgeId
   * @returns the Currencylabel dependent on edgeId
   */
  export function getCurrencyLabelByEdgeId(edgeId: string): Label {
    switch (edgeId) {
      default:
        return Label.CENT_PER_KWH;
    }
  }

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
      default:
        return Label.CENT_PER_KWH;
    }
  }

  export enum Label {
    OERE_PER_KWH = "Öre/kWh",
    CENT_PER_KWH = "Cent/kWh",
  }

  export enum Unit {
    CENT = "Cent",
  }
}

export enum EssStateMachine {
  UNDEFINED = -1, //
  START_BATTERY = 10, //
  START_BATTERY_INVERTER = 11, //
  STARTED = 12, //
  STOP_BATTERY_INVERTER = 20, //
  STOP_BATTERY = 21, //
  STOPPED = 22, //
  ERROR = 30,
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
      text: translate.instant("General.cancel"),
      role: "cancel",
    },
    ...(alertOptions?.buttons ?? []),
    ],
    cssClass: "alertController",
  });
  (await alert).present();
}
