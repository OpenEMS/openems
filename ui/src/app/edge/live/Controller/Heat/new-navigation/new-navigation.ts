import {CommonModule} from "@angular/common";
import { Component, inject } from "@angular/core";
import {ReactiveFormsModule} from "@angular/forms";
import {IonicModule} from "@ionic/angular";
import {FormlyModule} from "@ngx-formly/core";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedControllerHeat } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class ControllerHeatHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private readonly routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge, isMyPV: boolean, isAskoma: boolean, isReadOnly: boolean): OeFormlyView {
        return {
            title: component.alias,
            icon: { name: "flame", color: "normal", size: "normal" },
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            lines: [
                ...(isAskoma ? SharedControllerHeat.getAskomaIcon() : []),
                ...(isMyPV ? SharedControllerHeat.getMyPvIcon() : []),
                ...SharedControllerHeat.getFormlySharedLines(translate, component, isAskoma),
                ...(isMyPV && isReadOnly ? SharedControllerHeat.getMyPVInfoLine(translate) : []),
            ],
            component: component,
            edge: edge,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        // Check for specific factoryId
        const isMyPV =
            component.factoryId === "Heat.MyPv.AcThor9s" ||
          component.factoryId === "Heat.MyPv";
        const isAskoma = component.factoryId === "Heat.Askoma";
        const isReadOnly = component.properties?.readOnly === true;
        return ControllerHeatHomeComponent.generateView(this.translate, component, edge, isMyPV, isAskoma, isReadOnly);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);
        return SharedControllerHeat.getChannelAddresses(this.service, this.routeService, component);
    }
}
