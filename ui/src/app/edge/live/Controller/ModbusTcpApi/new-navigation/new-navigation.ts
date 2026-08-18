import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedControllerModbusTcpApiReadWrite } from "../shared/shared";

@Component({
    selector: "oe-controller-modbus-tcp-api",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerModbusTcpApiHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component | null,
    ): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        const channel = new ChannelAddress(component.id, "OverrideStatus");
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_API_MODBUSTCP_READWRITE",
            lines: [
                {
                    type: "value-from-channels-line",
                    channelsToSubscribe: [channel],
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.CURRENT_STATE"),
                    value: (currentData: CurrentData) =>
                        SharedControllerModbusTcpApiReadWrite.TO_OVERRIDE_STATUS_LABEL(translate)(
                            currentData.allComponents[channel.toString()],
                        ),
                },
            ],
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam<string>("componentId"));
        return ControllerModbusTcpApiHomeComponent.getFormlyGeneralView(this.translate, component);
    }
}
