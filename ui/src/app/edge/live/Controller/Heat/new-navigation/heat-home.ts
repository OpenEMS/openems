import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { EnergySchedulerV2 } from "../../../../../shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "../../../../../shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "../../../../../shared/components/shared/converter";
import { LiveDataService } from "../../../livedataservice";
import { CONVERT_CHANNEL_MODE_TO_LABEL, SharedControllerHeat } from "../shared/shared";
import { HeatModeChartComponent } from "./chart/heat-mode-chart";
import { HeatPowerChartComponent } from "./chart/heat-power-chart";
import { HeatStatusChartComponent } from "./chart/heat-status-chart";
import { HeatConverter } from "./converter";

@Component({
    selector: "oe-controller-heat-new-navigation",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerHeatHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private readonly routeService: RouteService = inject(RouteService);

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
    ): OeFormlyView {
        return {
            title: component.alias,
            icon: { name: "oe-heating-element", color: "normal", size: "normal" },
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            useDefaultPrefix: false,
            lines: ControllerHeatHomeComponent.getLines(translate, component, edge, energyScheduler),
            component: component,
            edge: edge,
        };
    }

    private static getLines(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
    ): OeFormlyField[] {
        if (energyScheduler.schedule === GetSchedule.Response.empty) {
            return SharedControllerHeat.getLegacyViewLines(translate, component);
        }
        const statusLines = (): OeFormlyField[] => [
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATUS"),
                channel: component.id + "/Status",
                converter: HeatConverter.CONVERT_POWER_2_HEAT_STATE(translate),
                style: {
                    name: { fontSize: "large" },
                    value: { fontSize: "large" },
                },
            },
            {
                type: "component-line",
                component: HeatStatusChartComponent,
                inputs: {
                    edge,
                    refresh: false,
                    data: energyScheduler.schedule,
                    componentId: component.id,
                },
            },
        ];
        const powerLines = (): OeFormlyField[] => [
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
                channel: component.id + "/ActivePower",
                converter: Converter.POWER_IN_WATT,
                style: {
                    name: { fontSize: "large" },
                    value: { fontSize: "large" },
                },
            },
            {
                type: "component-line",
                component: HeatPowerChartComponent,
                inputs: {
                    edge,
                    refresh: false,
                    data: energyScheduler.schedule,
                    componentId: component.id,
                },
            },
        ];
        const modeLines = (): OeFormlyField[] => [
            {
                type: "channel-line" as const,
                name: translate.instant("GENERAL.MODE"),
                channel: component.id + "/Mode",
                converter: (value: number | null) => CONVERT_CHANNEL_MODE_TO_LABEL(translate)(value),
                filter: (value: number | null) => value != null,
                style: {
                    name: { fontSize: "large" },
                    value: { fontSize: "large" },
                },
            },
            {
                type: "component-line",
                component: HeatModeChartComponent,
                inputs: {
                    edge,
                    refresh: false,
                    data: energyScheduler.schedule,
                    componentId: component.id,
                },
            },
        ];
        return [
            ControllerHeatHomeComponent.getTemperatureLine(translate, component),
            {
                type: "component-line",
                component: TimeLineChartComponent,
                inputs: {
                    data: energyScheduler.schedule,
                },
            },
            ...statusLines(),
            ...powerLines(),
            ...modeLines(),
        ];
    }

    private static getTemperatureLine(translate: TranslateService, component: EdgeConfig.Component): OeFormlyField {
        return {
            type: "channel-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE"),
            channel: component.id + "/Temperature",
            converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
            style: {
                name: { fontSize: "large" },
                value: { fontSize: "large" },
            },
        };
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);
        const energyScheduler = new EnergySchedulerV2(config);
        await energyScheduler.updateSchedule(edge, this.service.websocket);

        return ControllerHeatHomeComponent.generateView(this.translate, component, edge, energyScheduler);
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
