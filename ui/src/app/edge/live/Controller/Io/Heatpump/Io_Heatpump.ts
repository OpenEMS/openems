// @ts-strict-ignore
import { Component } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";

import { Controller_Io_HeatpumpModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
  selector: "Controller_Io_Heatpump",
  templateUrl: "./Io_Heatpump.html",
  standalone: false,
})
export class Controller_Io_HeatpumpComponent extends AbstractFlatWidget {

  private static PROPERTY_MODE: string = "_PropertyMode";

  public override component: EDGE_CONFIG.COMPONENT | null = null;
  public status: BehaviorSubject<{ name: string }> = new BehaviorSubject(null);
  public isConnectionSuccessful: boolean;
  public mode: string;
  public statusValue: number;

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: Controller_Io_HeatpumpModalComponent,
      componentProps: {
        edge: THIS.EDGE,
        component: THIS.COMPONENT,
        status: THIS.STATUS,
      },
    });
    MODAL.ON_DID_DISMISS().then(() => {
      THIS.SERVICE.GET_CONFIG().then(config => {
        THIS.COMPONENT = CONFIG.COMPONENTS[THIS.COMPONENT_ID];
      });
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses() {
    return [
      new ChannelAddress(THIS.COMPONENT.ID, "Status"),
      new ChannelAddress(THIS.COMPONENT.ID, "State"),
      new ChannelAddress(THIS.COMPONENT.ID, Controller_Io_HeatpumpComponent.PROPERTY_MODE),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.IS_CONNECTION_SUCCESSFUL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/State"] != 3 ? true : false;

    // Status
    switch (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/Status"]) {
      case -1:
        THIS.STATUS_VALUE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED");
        break;
      case 0:
        THIS.STATUS_VALUE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK");
        break;
      case 1:
        THIS.STATUS_VALUE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION");
        break;
      case 2:
        THIS.STATUS_VALUE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC");
        break;
      case 3:
        THIS.STATUS_VALUE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM");
        break;
    }

    // Mode
    switch (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + Controller_Io_HeatpumpComponent.PROPERTY_MODE]) {
      case "AUTOMATIC": {
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.AUTOMATIC");
        break;
      }
      case "MANUAL": {
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.MANUALLY");
        break;
      }
    }
  }

}
