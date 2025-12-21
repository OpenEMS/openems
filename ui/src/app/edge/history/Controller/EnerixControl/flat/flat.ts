import { Component } from "@angular/core";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
    selector: "enerixControlWidget",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected TIME_CONVERTER = this.Converter.FORMAT_SECONDS_TO_DURATION("de");

    protected override getChannelAddresses(): ChannelAddress[] {

        return [
            new ChannelAddress(this.component.id, "CumulatedInactiveTime"),
            new ChannelAddress(this.component.id, "CumulatedNoDischargeTime"),
            new ChannelAddress(this.component.id, "CumulatedChargeFromGridTime"),
        ];
    }
}
