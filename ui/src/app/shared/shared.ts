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
  'oe-storage': 'assets/img/icon/storage.svg',
});

export class UserPermission {
  public static isUserAllowedToSeeOverview(user: User): boolean {

    if (Role.isAtLeast(user.globalRole, Role.INSTALLER)) {
      return true;
    }

    return user.hasMultipleEdges;
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

  export enum Label {
    OERE_PER_KWH = "Öre/kWh",
    CENT_PER_KWH = "Cent/kWh"
  }
}
