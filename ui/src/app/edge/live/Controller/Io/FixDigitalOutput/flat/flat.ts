import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerFixDigitalOutputModalComponent } from "../modal/modal";

@Component({
    selector: "oe-controller-io-fix-digital-output",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerFixDigitalOutputComponent extends AbstractFlatWidget {
    public state: string = "-";
    public outputChannel: string | null = null;
    protected modalComponent: Modal | null = null;
    private readonly routeService: RouteService = inject(RouteService);
    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: ControllerFixDigitalOutputModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        };
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = this.component ?? config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        this.outputChannel = component.getPropertyFromComponent("outputChannelAddress");
        if (this.outputChannel == null) {
            return [];
        }
        return [ChannelAddress.fromString(this.outputChannel)];
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.outputChannel == null) {
            return;
        }
        const channel = currentData.allComponents[this.outputChannel];

        if (channel != null) {
            if (channel == 1) {
                this.state = this.translate.instant("GENERAL.ON");
            } else if (channel == 0) {
                this.state = this.translate.instant("GENERAL.OFF");
            }
        }
    }
}
