// @ts-strict-ignore
export { Edge } from "./edge/edge";
export { EdgeConfig } from "./edge/edgeconfig";
export { Logger } from "./service/logger";
export { Service } from "./service/service";
export { Utils } from "./service/utils";
export { Websocket } from "./service/websocket";
export { ChannelAddress } from "./type/channeladdress";
export { CurrentData } from "./type/currentdata";
export { BackupEnable, BatteryMode, BatteryStateMachine, DredCmd, GoodWe, GridMode, SafetyCountryCode } from "./type/general";
export { SystemLog } from "./type/systemlog";
export { Widget, WidgetFactory, WidgetNature, Widgets } from "./type/widget";

import { addIcons } from 'ionicons';
import { Edge } from "./edge/edge";
import { User } from "./jsonrpc/shared";
import { DefaultTypes } from "./service/defaulttypes";
import { Role } from "./type/role";

addIcons({
  'oe-consumption': 'assets/img/icon/consumption.svg',
  'oe-evcs': 'assets/img/icon/evcs.svg',
  'oe-grid': 'assets/img/icon/grid.svg',
  'oe-grid-storage': 'assets/img/icon/gridStorage.svg',
  'oe-offgrid': 'assets/img/icon/offgrid.svg',
  'oe-production': 'assets/img/icon/production.svg',
  'oe-storage': 'assets/img/icon/storage.svg',
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
   * Checks if user is allowed to see {@link HomeServiceAssistentComponent}
   * Producttype needs to be Producttype.HOME or Producttype.HOME_20_30 and globalRole needs to be admin
   *
   * @param user the current user
   * @returns true, if user is at least admin
   */
  public static isUserAllowedToSeeHomeAssistent(user: User, edge: Edge): boolean {
    const isProductTypeAllowed = (() => {
      switch (edge.producttype) {
        case Producttype.HOME:
        case Producttype.HOME_20_30:
          return true;
        default:
          return false;
      }
    })();
    return Role.isAtLeast(user.globalRole, Role.ADMIN) && isProductTypeAllowed;
  }

  /**
  * Checks if user is allowed to see {@link ServiceAssistantComponent}
  * Producttype needs to be Commercial and globalRole needs to be admin
  *
  * @param user the current user
  * @returns true, if user is at least admin
  */
  public static isUserAllowedToSeeCommercialServiceAssistent(user: User, edge: Edge) {
    const isProductTypeAllowed = (() => {
      switch (edge.producttype) {
        case Producttype.COMMERCIAL_30:
        case Producttype.COMMERCIAL_50:
        case Producttype.COMMERCIAL_40_45:
          return true;
        default:
          return false;
      }
    })();
    return Role.isAtLeast(user.globalRole, Role.ADMIN) && isProductTypeAllowed;
  }

  /**
  * Checks if user is allowed to see {@link SystemRestartComponent}
  *
  * @param user the current user
  * @returns true, if user is at least {@link Role.ADMIN} and edge version is at least 2024.2.2
  */
  public static isAllowedToSeeSystemRestart(user: User, edge: Edge) {
    const isAllowed = edge?.isVersionAtLeast('2024.2.2');
    return Role.isAtLeast(user?.globalRole, Role.OWNER) && isAllowed;
  }
}

export enum Producttype {
  HOME = 'home',
  HOME_20_30 = 'Home 20 & 30',
  COMMERCIAL_30 = 'Commercial 30-Serie',
  COMMERCIAL_50 = 'Commercial 50-Serie',
  COMMERCIAL_40_45 = 'COMMERCIAL 40-45'
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
      case 'fems17289':
        return Label.OERE_PER_KWH;
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
      case 'SEK':
        return Label.OERE_PER_KWH;
      default:
        return Label.CENT_PER_KWH;
    }
  }

  export enum Label {
    OERE_PER_KWH = "Öre/kWh",
    CENT_PER_KWH = "Cent/kWh"
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
