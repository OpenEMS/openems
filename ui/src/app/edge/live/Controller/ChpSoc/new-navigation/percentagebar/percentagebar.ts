import { Component, inject } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "oe-controller-chp-percentagebar",
    templateUrl: "./percentagebar.html",
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class CommonChpPercentagebarComponent extends AbstractModal {
    public highThresholdValue: number | null = null;
    public lowThresholdValue: number | null = null;
    public thresholdDelta: number | null = null;
    public inputChannel: ChannelAddress | null = null;
    public inputChannelValue: number | null = null;
    public chpController: EdgeConfig.Component | null = null;

    protected readonly routeService: RouteService = inject(RouteService);

    protected override getChannelAddresses(): ChannelAddress[] {
        this.chpController ??= this.getComponent();
        if (this.chpController == null) {
            return [];
        }
        const inputChannel = ChannelAddress.fromStringSafely(
            this.chpController.getPropertyFromComponent("inputChannelAddress"),
        );

        return [
            ...(inputChannel ? [inputChannel] : []),
            new ChannelAddress(this.chpController.id, "_PropertyLowThreshold"),
            new ChannelAddress(this.chpController.id, "_PropertyHighThreshold"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.chpController ??= this.getComponent();
        if (this.chpController == null) {
            return;
        }

        this.highThresholdValue = currentData.allComponents[this.chpController.id + "/_PropertyHighThreshold"];
        this.lowThresholdValue = currentData.allComponents[this.chpController.id + "/_PropertyLowThreshold"];

        this.inputChannel = ChannelAddress.fromStringSafely(
            this.chpController.getPropertyFromComponent("inputChannelAddress"),
        );

        if (this.inputChannel != null) {
            this.inputChannelValue = currentData.allComponents[this.inputChannel.toString()];
        }

        if (this.highThresholdValue == null || this.lowThresholdValue == null) {
            this.thresholdDelta = null;
            return;
        }
        const delta = this.highThresholdValue - this.lowThresholdValue;
        this.thresholdDelta = Math.max(delta, 0);
    }

    private getComponent(): EdgeConfig.Component {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        return component;
    }
}
