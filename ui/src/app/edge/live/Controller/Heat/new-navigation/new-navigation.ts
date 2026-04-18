import { Component, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedControllerHeat } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerHeatHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private route: ActivatedRoute = inject(ActivatedRoute);
    private routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge, isMyPv: boolean): OeFormlyView {
        return SharedControllerHeat.getFormlyView(translate, component, edge, isMyPv);
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        // Check for specific factoryId
        const isMyPV = component.factoryId === "Heat.MyPv.AcThor9s";

        return ControllerHeatHomeComponent.generateView(this.translate, component, edge, isMyPV);
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
