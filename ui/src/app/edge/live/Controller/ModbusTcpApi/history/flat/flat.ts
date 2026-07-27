import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
    selector: "modbusTcpApiWidget",
    templateUrl: "./flat.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class ModbusTcpApiHistoryFlatComponent extends AbstractFlatWidget {
    protected TIME_CONVERTER = this.Converter.FORMAT_SECONDS_TO_DURATION("de");

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this.component == null) {
            return [];
        }

        return [
            new ChannelAddress(this.component.id, "CumulatedInactiveTime"),
            new ChannelAddress(this.component.id, "CumulatedActiveTime"),
        ];
    }
}
