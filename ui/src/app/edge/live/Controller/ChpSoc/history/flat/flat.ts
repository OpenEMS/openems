import { ChangeDetectionStrategy, Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
    selector: "oe-controller-chp-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class FlatComponent extends AbstractFlatWidget {
    protected TIME_CONVERTER = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this.component == null) {
            return [];
        }

        return [new ChannelAddress(this.component.id, "CumulatedActiveTime")];
    }
}
