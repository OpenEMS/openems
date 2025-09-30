import { Component } from "@angular/core";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
  selector: "modbusTcpApiWidget",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  protected TIME_CONVERTER = THIS.CONVERTER.FORMAT_SECONDS_TO_DURATION("de");

  protected override getChannelAddresses(): ChannelAddress[] {

    return [
      new ChannelAddress(THIS.COMPONENT.ID, "CumulatedInactiveTime"),
      new ChannelAddress(THIS.COMPONENT.ID, "CumulatedActiveTime"),
    ];
  }
}
