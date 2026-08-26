import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { HeatpumpMode, SharedControllerIoHeatpump } from "../shared/shared";

@Component({
    selector: "oe-controller-io-heatpump-home",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerIoHeatpumpHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private readonly route: ActivatedRoute = inject(ActivatedRoute);
    private readonly component: EdgeConfig.Component | null = null;

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        config: EdgeConfig | null,
    ): OeFormlyView {
        const lines: OeFormlyField[] = [];
        const consumptionMeter = SharedControllerIoHeatpump.getConsumptionMeter(config, component);

        lines.push({
            type: "image-line",
            img: {
                url: "icons/component/heatpump.svg",
                width: 20,
                height: 20,
                color: "var(--ion-color-heatpump-base)",
                style: {
                    maxWidth: "20rem",
                    justifySelf: "center",
                    paddingBottom: "var(--ion-padding)",
                },
            },
        });

        if (consumptionMeter) {
            lines.push({
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
                channel: consumptionMeter.id + "/ActivePower",
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATUS"),
                channel: component.id + "/Status",
                converter: Converter.HEAT_PUMP_STATES(translate),
            },
            {
                type: "value-from-channels-line",
                name: translate.instant("GENERAL.MODE"),
                value: (currentData) => {
                    const isTimeScheduleTaskActive =
                        currentData.allComponents[component.id + "/IsTimeScheduleTaskActive"] ?? false;
                    const mode = isTimeScheduleTaskActive
                        ? HeatpumpMode.TIME_SCHEDULE
                        : currentData.allComponents[component.id + "/_PropertyMode"];
                    return Converter.CONTROLLER_PROPERTY_MODES(translate)(mode);
                },
                channelsToSubscribe: [
                    new ChannelAddress(component.id, "_PropertyMode"),
                    new ChannelAddress(component.id, "IsTimeScheduleTaskActive"),
                ],
            },
        );
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_HEAT_PUMP_SG_READY",
            lines: lines,
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        const component = this.component ?? this.getComponent();

        return ControllerIoHeatpumpHomeComponent.getFormlyGeneralView(this.translate, component, config);
    }
}
