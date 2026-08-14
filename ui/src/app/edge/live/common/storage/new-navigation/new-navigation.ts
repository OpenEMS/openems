import { CommonModule } from "@angular/common";
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { LiveDataService } from "../../../livedataservice";
import { SharedStorage } from "../shared/shared";
import { ChargeDischargeChartComponent } from "./chart/charge-discharge-chart";
import { ModeChartComponent } from "./chart/mode-chart";
import { SocChartComponent } from "./chart/soc-chart";
import { CommonStoragePercentagebarComponent } from "./percentagebar/percentagebar";

@Component({
    selector: "oe-common-storage",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class CommonStorageHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static async getFormlyGeneralView(
        translate: TranslateService,
        service: Service,
        edge: Edge,
        config: EdgeConfig,
        energyScheduler: EnergySchedulerV2,
    ): Promise<OeFormlyView> {
        return {
            title: translate.instant("GENERAL.STORAGE_SYSTEM"),
            helpKey: "REDIRECT.COMMON_STORAGE",
            lines: await CommonStorageHomeComponent.getLines(translate, service, edge, config, energyScheduler),
            component: new EdgeConfig.Component(),
            useDefaultPrefix: false,
            isCommonWidget: true,
        };
    }

    private static async getLines(
        translate: TranslateService,
        service: Service,
        edge: Edge,
        config: EdgeConfig,
        energyScheduler: EnergySchedulerV2,
    ): Promise<OeFormlyField[]> {
        await energyScheduler?.updateSchedule(edge, service.websocket);
        const essComponents: EdgeConfig.Component[] = SharedStorage.getEssComponents(config);
        const emergencyReserveComponents: {
            [essId: string]: EdgeConfig.Component;
        } = config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});
        const prepareBatteryExtensionCtrl: { [essId: string]: EdgeConfig.Component } = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});

        const controllerLines = essComponents.reduce((arr: OeFormlyField[] = [], ess, i) => {
            if (essComponents.length > 1) {
                arr.push({
                    type: "name-line",
                    name: Name.METER_ALIAS_OR_ID(ess),
                });
            }

            const emergencyReserveCtrl = emergencyReserveComponents[ess.id];
            arr.push(
                {
                    type: "component-line",
                    component: CommonStoragePercentagebarComponent,
                    inputs: {
                        essComponentId: ess.id,
                        emergencyReserveController: emergencyReserveCtrl,
                    },
                },
                ...SharedStorage.getChargeDischargeLinesInKw(ess, config, translate),
            );

            const prepareBatteryExtensionCtrlForEss =
                ess.id in prepareBatteryExtensionCtrl ? prepareBatteryExtensionCtrl[ess.id] : null;

            if (prepareBatteryExtensionCtrlForEss !== null) {
                arr.push(
                    { type: "horizontal-line" },
                    {
                        type: "value-from-channels-line",
                        channelsToSubscribe: [
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/_PropertyIsRunning"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsBlockingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsChargingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsDischargingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsInReferenceCycle"),
                            ChannelAddress.fromString(
                                prepareBatteryExtensionCtrlForEss.id + "/_PropertyTargetTimeSpecified",
                            ),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/_PropertyTargetTime"),
                        ],
                        singleLine: true,
                        value: (currentData: CurrentData) =>
                            SharedStorage.getBatteryCapacityExtensionStatus(
                                translate,
                                currentData,
                                prepareBatteryExtensionCtrlForEss.id,
                            )?.text ?? null,
                        filter: (currentData: CurrentData) =>
                            SharedStorage.getBatteryCapacityExtensionStatus(
                                translate,
                                currentData,
                                prepareBatteryExtensionCtrlForEss.id,
                            )?.text != null,
                    },
                );
            }

            if (i < essComponents.length - 1) {
                arr.push({
                    type: "horizontal-line",
                });
            }

            return arr;
        }, []);

        const lines: OeFormlyField[] = [];

        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
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
                    name: translate.instant("GENERAL.POWER"),
                    channel: new ChannelAddress("_sum", "EssDischargePower").toString(),
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                    converter: ESS_CHARGE_OR_DISCHARGE(translate),
                },
                {
                    type: "component-line",
                    component: ChargeDischargeChartComponent,
                    inputs: {
                        edge: edge,
                        refresh: false,
                        data: energyScheduler.schedule,
                    },
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.SOC"),
                    channel: new ChannelAddress("_sum", "EssSoc").toString(),
                    converter: Converter.STATE_IN_PERCENT,
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large", textAlign: "right" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: SocChartComponent,
                    inputs: {
                        edge: edge,
                        refresh: false,
                        data: energyScheduler.schedule,
                    },
                },
            );

            if (config.hasFactories(["Controller.Ess.Time-Of-Use-Tariff"])) {
                lines.push(
                    {
                        type: "channel-line",
                        name: translate.instant("GENERAL.MODE"),
                        channel: new ChannelAddress("ctrlEssTimeOfUseTariff0", "StateMachine").toString(),
                        converter: Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(translate),
                        style: {
                            name: { fontSize: "large" },
                            value: { fontSize: "large" },
                        },
                        cssClass: "ion-padding-top",
                    },
                    {
                        type: "component-line",
                        component: ModeChartComponent,
                        inputs: {
                            edge: edge,
                            refresh: false,
                            data: energyScheduler.schedule,
                        },
                    },
                );

                lines.push({
                    type: "horizontal-line",
                });
            }

            lines.push({
                type: "name-line",
                name: translate.instant("GENERAL.DETAILS"),
                style: {
                    name: { fontSize: "large" },
                },
                cssClass: "ion-padding-top",
            });
        }

        lines.push(...controllerLines);
        return lines;
    }

    public override getFormGroup(): FormGroup {
        return new FormGroup({
            soc: new FormControl(null),
        });
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const energy = new EnergySchedulerV2(config);

        return await CommonStorageHomeComponent.getFormlyGeneralView(
            this.translate,
            this.service,
            edge,
            config,
            energy,
        );
    }


    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithValue(this.form, "soc", currentData.allComponents["_sum/EssSoc"]);
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        return Promise.resolve([new ChannelAddress("_sum", "EssSoc")]);
    }

}

export const ESS_CHARGE_OR_DISCHARGE =
    (translate: TranslateService): Converter =>
    (raw): string => {
        const displayText = (power: string, color: "danger" | "success" | "inherit", text: string): string => {
            return `<span>${power}&nbsp;<ion-label color="${color}">${text}</ion-label></span>`;
        };

        return Converter.IF_NUMBER(raw, (value) => {
            if (value > 0) {
                return displayText(
                    Converter.POWER_IN_KILO_WATT(value),
                    "danger",
                    translate.instant("GENERAL.DISCHARGE"),
                );
            } else if (value < 0) {
                return displayText(
                    Converter.POWER_IN_KILO_WATT(Math.abs(value)),
                    "success",
                    translate.instant("GENERAL.CHARGE"),
                );
            } else {
                return Converter.POWER_IN_KILO_WATT(value);
            }
        });
    };
