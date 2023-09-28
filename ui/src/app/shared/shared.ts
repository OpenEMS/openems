export { Edge } from "./edge/edge";
export { EdgeConfig } from "./edge/edgeconfig";
export { Logger } from "./service/logger";
export { Service } from "./service/service";
export { Utils } from "./service/utils";
export { Websocket } from "./service/websocket";
export { ChannelAddress } from "./type/channeladdress";
export { CurrentData } from "./type/currentdata";
export { GridMode } from "./type/general";
export { SystemLog } from "./type/systemlog";
export { Widget, WidgetFactory, WidgetNature, Widgets } from "./type/widget";

import { Edge } from "./edge/edge";
import { User } from "./jsonrpc/shared";
import { Role } from "./type/role";

import { addIcons } from 'ionicons';

addIcons({
  'oe-consumption': 'assets/img/icon/consumption.svg',
  'oe-evcs': 'assets/img/icon/evcs.svg',
  'oe-grid': 'assets/img/icon/grid.svg',
  'oe-grid-storage': 'assets/img/icon/gridStorage.svg',
  'oe-offgrid': 'assets/img/icon/offgrid.svg',
  'oe-production': 'assets/img/icon/production.svg',
  'oe-storage': 'assets/img/icon/storage.svg'
});

export class UserPermission {
  public static isUserAllowedToSeeOverview(user: User): boolean {

    if (Role.isAtLeast(user.globalRole, Role.INSTALLER)) {
      return true;
    }

    return user.hasMultipleEdges;
  }

  /**
   * Checks if user is allowed to see {@link HomeServiceAssistentComponent}
   * Producttype needs to be home and globalRole needs to be admin
   * 
   * @param user the current user
   * @returns true, if user is at least admin
   */
  public static isUserAllowedToSeeHomeAssistent(user: User, edge: Edge): boolean {
    return Role.isAtLeast(user.globalRole, Role.ADMIN) && edge.producttype === 'home';
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
      case 'fems17289':
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
  ERROR = 30
}