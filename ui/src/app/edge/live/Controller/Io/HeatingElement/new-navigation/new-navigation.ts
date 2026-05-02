import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { SharedControllerIoHeatingElement } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerIoHeatingElementHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, mode: Mode): OeFormlyView {
        const lines: OeFormlyField[] = [];

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.MODE"),
            channel: component.id + "/_PropertyMode",
            converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
        }, {
            type: "value-from-channels-line",
            name: translate.instant("GENERAL.STATE"),
            channelsToSubscribe: [new ChannelAddress(component.id, "Status")],
            value: (currentData: CurrentData) => {
                const runState = currentData.allComponents[component.id + "/Status"];
                return Converter.CONVERT_HEATING_ELEMENT_RUNSTATE(translate)(runState);
            },
        });

        if (mode !== Mode.MANUAL_OFF) {
            lines.push({
                type: "value-from-channels-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.ACTIVE_LEVEL"),
                channelsToSubscribe: [new ChannelAddress(component.id, "Level")],
                value: (currentData: CurrentData) => {
                    const level = currentData.allComponents[component.id + "/Level"];
                    return "Level " + level;
                },
            });
        }

        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            icon: { name: "flame", color: "normal", size: "large" },
            lines: lines,
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        const mode = component.properties.mode;
        return ControllerIoHeatingElementHomeComponent.generateView(this.translate, component, mode);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedControllerIoHeatingElement.getChannelAddresses(component);
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
