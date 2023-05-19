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

export class UserPermission {
  public static isUserAllowedToSeeOverview(user: User): boolean {
    return Role.isAtLeast(user.globalRole, Role.INSTALLER);
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
    CENT_PER_KWH = "Cent/kWh"
  }
}

