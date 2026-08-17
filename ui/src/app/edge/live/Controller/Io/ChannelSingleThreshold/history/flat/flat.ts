import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsModule } from "src/app/shared/components/components.module";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "channelsingleThresholdWidget",
    templateUrl: "./flat.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule, ComponentsModule],
})
export class FlatComponent extends AbstractFlatWidget {
    protected controllerLines: { name: string; channelAddress: string }[] = [];
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());

    protected override getChannelAddresses(): ChannelAddress[] {
        AssertionUtils.assertIsDefined(this.config);

        const controllersByFactory = this.config.getComponentsByFactory("Controller.IO.ChannelSingleThreshold") ?? [];

        const controllers = controllersByFactory.filter((controller) => controller != null);

        const channelAddresses: ChannelAddress[] = [];

        for (const controller of controllers) {
            const outputChannelAddress = controller.getPropertyFromComponent<string>("outputChannelAddress");

            const output = outputChannelAddress == null ? null : ChannelAddress.fromString(outputChannelAddress);
            this.controllerLines.push({
                name: this.getDisplayName(controller, output),
                channelAddress: controller.id + "/CumulatedActiveTime",
            });
            channelAddresses.push(new ChannelAddress(controller.id, "CumulatedActiveTime"));
        }
        return channelAddresses;
    }

    private getDisplayName(controller: EdgeConfig.Component, output: ChannelAddress | null): string {
        return controller.id === controller.alias ? (output?.channelId ?? controller.alias) : controller.alias;
    }
}
