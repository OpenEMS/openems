import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { UserService } from "src/app/shared/service/user.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { ControllerBraiinsShared } from "../shared/shared";
import { ControllerBraiinsModeChartComponent } from "./chart/mode-chart";
import { ControllerBraiinsManagedConsumptionChartComponent } from "./chart/power-chart";

@Component({
    selector: "oe-controller-braiins-home",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerBraiinsHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private component: EdgeConfig.Component | null = null;
    private readonly userService = inject(UserService);

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);
        this.component = component;

        const energyScheduler = new EnergySchedulerV2(config);
        const lines = await this.getLines(component, edge, energyScheduler);

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            icon: {
                name: "logo-bitcoin",
                color: "rgb(247, 148, 29)",
                size: "large",
            },
            lines,
            component,
            edge,
        };
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = this.component ?? config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        return [new ChannelAddress(component.id, ControllerBraiinsShared.PROPERTY_MODE)];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const braiinsComponent = this.component;
        AssertionUtils.assertIsDefined(braiinsComponent);

        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(braiinsComponent.id, ControllerBraiinsShared.PROPERTY_MODE),
        );
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
        });
    }

    private async getLines(
        component: EdgeConfig.Component,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
    ): Promise<OeFormlyField[]> {
        await energyScheduler?.updateSchedule(edge, this.service.websocket);
        const user = this.userService.currentUser();
        const lines: OeFormlyField[] = [];

        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
            // TODO INTERSOLAR
            if (user?.id == "intersolar@fenecon.de" || edge.id == "fems888" || edge.id == "fems4") {
                const energyToday = energyScheduler.schedule.calculateEnergyFromPower("today", {
                    eshsId: component.id,
                });
                const energyTomorrow = energyScheduler.schedule.calculateEnergyFromPower("tomorrow", {
                    eshsId: component.id,
                });
                lines.push({
                    type: "stats-line",
                    stats: [
                        {
                            name: this.translate.instant("EDGE.HISTORY.TODAY"),
                            value: energyToday.history,
                            unit: "kWh",
                            predictionValue: energyToday.prediction,
                        },
                        {
                            name: this.translate.instant("EDGE.HISTORY.TOMORROW"),
                            value: energyTomorrow.prediction,
                            unit: "kWh",
                        },
                    ],
                });
            }

            lines.push(
                {
                    type: "component-line",
                    component: TimeLineChartComponent,
                    inputs: {
                        data: energyScheduler.schedule,
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "channel-line",
                    name: this.translate.instant("GENERAL.POWER"),
                    channel: new ChannelAddress(component.id, ControllerBraiinsShared.ACTIVE_POWER).toString(),
                    converter: Converter.POWER_IN_KILO_WATT,
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: ControllerBraiinsManagedConsumptionChartComponent,
                    inputs: {
                        edge,
                        refresh: false,
                        data: energyScheduler.schedule,
                        componentId: component.id,
                    },
                },
                {
                    type: "channel-line",
                    name: this.translate.instant("BRAIINS_SINGLE.MODE.ACTIVE_MODE"),
                    channel: new ChannelAddress(component.id, ControllerBraiinsShared.EFFECTIVE_MODE).toString(),
                    converter: ControllerBraiinsShared.CONVERT_TO_MODE_LABEL(this.translate),
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: ControllerBraiinsModeChartComponent,
                    inputs: {
                        edge,
                        refresh: false,
                        data: energyScheduler.schedule,
                        componentId: component.id,
                    },
                },
            );
        }

        return lines;
    }
}
