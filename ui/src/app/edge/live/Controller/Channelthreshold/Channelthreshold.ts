// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Icon } from "src/app/shared/type/widget";

import { ChannelAddress, CurrentData } from "../../../../shared/shared";

@Component({
  selector: "Controller_Channelthreshold",
  templateUrl: "./CHANNELTHRESHOLD.HTML",
  standalone: false,
})
export class Controller_ChannelthresholdComponent extends AbstractFlatWidget {

  public outputChannel: ChannelAddress;
  public icon: Icon = {
    name: "",
    size: "large",
    color: "normal",
  };
  public state: string = "?";

  protected override getChannelAddresses() {
    THIS.OUTPUT_CHANNEL = CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.PROPERTIES["outputChannelAddress"]);
    return [THIS.OUTPUT_CHANNEL];
  }
  protected override onCurrentData(currentData: CurrentData) {
    const channel = CURRENT_DATA.ALL_COMPONENTS[THIS.OUTPUT_CHANNEL.TO_STRING()];
    if (channel != null) {
      if (channel == 1) {
        THIS.ICON.NAME = "radio-button-on-outline";
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.ON");
      } else if (channel == 0) {
        THIS.ICON.NAME = "radio-button-off-outline";
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.OFF");
      }
    } else {
      THIS.ICON.NAME = "help-outline";
    }
  }
}
