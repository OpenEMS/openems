import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { SharedIoChannelSingleThreshold } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerIoChannelSingleThresholdHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;
    private readonly websocket: Websocket = inject(Websocket);

    public static async generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        websocket: Websocket,
    ): Promise<OeFormlyView> {
        const lines: OeFormlyField[] = [];
        const outputChannel = component.getPropertyFromComponent<string[]>("outputChannelAddress");
        const inputChannel = component.getPropertyFromComponent<string>("inputChannelAddress");

        if (outputChannel != null && inputChannel != null) {
            const inputChannelAddress = ChannelAddress.fromString(inputChannel);
            const channelAddress = inputChannelAddress != null ? [inputChannelAddress] : [];
            const getUnit = await SharedIoChannelSingleThreshold.createUnitResolver(edge, websocket, channelAddress);
            lines.push(
                ...SharedIoChannelSingleThreshold.getFormlyHomeLines(
                    translate,
                    component,
                    outputChannel,
                    inputChannel,
                    getUnit,
                ),
            );
        }

        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD",
            icon: { name: "aperture-outline", color: "primary", size: "large" },
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        return ControllerIoChannelSingleThresholdHomeComponent.generateView(
            this.translate,
            component,
            edge,
            this.websocket,
        );
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        this.component ??= this.getComponent();
        return SharedIoChannelSingleThreshold.getChannelAddresses(this.service, this.routeService, this.component);
    }
}
