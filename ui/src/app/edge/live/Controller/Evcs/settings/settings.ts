import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { CONVERT_EVCS_STATUS } from "../new-navigation/new-navigation";
import { SharedEvcs } from "../shared/shared";

type EvcsSettingsViewModel = {
    chargeMode: "FORCE_CHARGE" | "EXCESS_POWER" | "OFF" | null;
    minGuarantee: boolean | null;
    energyLimit: boolean | null;
    isReadWrite: boolean | null;
};

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class EvcsSettingsComponent extends AbstractFormlyComponent<EvcsSettingsViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private numberOfPhases: number = 3;
    private minChargePower: number = 4140; // Default, updated from hardware
    private maxChargePower: number = 22100; // Default, updated from hardware

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        ctrl: EdgeConfig.Component | null,
        minChargePower: number,
        maxChargePower: number,
    ): OeFormlyView<EvcsSettingsViewModel> {
        const IS_NOT_READ_WRITE = (el: EvcsSettingsViewModel) => !el.isReadWrite;
        const IS_NOT_FORCE_CHARGE = (el: EvcsSettingsViewModel) => el.chargeMode !== "FORCE_CHARGE";
        const IS_NOT_EXCESS_POWER = (el: EvcsSettingsViewModel) => el.chargeMode !== "EXCESS_POWER";
        const IS_OFF_OR_NULL = (el: EvcsSettingsViewModel) => el.chargeMode === "OFF" || el.chargeMode == null;
        const IS_NO_MIN_GUARANTEE = (el: EvcsSettingsViewModel) => el.chargeMode !== "EXCESS_POWER" || !el.minGuarantee;
        const IS_NO_ENERGY_LIMIT = (el: EvcsSettingsViewModel) => IS_OFF_OR_NULL(el) || !el.energyLimit;

        const lines: OeFormlyField<EvcsSettingsViewModel>[] = [
            // Read-only status
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.STATUS"),
                channel: component.id + "/State",
                converter: CONVERT_EVCS_STATUS(translate),
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.UNCONTROLLABLE"),
                hide: (el) => el.isReadWrite || ctrl != null,
            },
            {
                type: "horizontal-line",
                hide: IS_NOT_READ_WRITE,
            },
            // Charge mode selection
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("GENERAL.MODE"),
                controlName: "chargeMode",
                buttons: [
                    {
                        name: translate.instant("GENERAL.MANUALLY"),
                        value: "FORCE_CHARGE",
                        icon: {
                            color: "success",
                            name: "options-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("GENERAL.AUTOMATIC"),
                        value: "EXCESS_POWER",
                        icon: {
                            color: "primary",
                            name: "sunny-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("GENERAL.OFF"),
                        value: "OFF",
                        icon: {
                            color: "danger",
                            name: "power-outline",
                            size: "medium",
                        },
                    },
                ],
                hide: (el) => IS_NOT_READ_WRITE(el) || ctrl == null,
            },
            { type: "horizontal-line" },
            // ── Force charge settings ──────────────────────────────────
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_CHARGE_MODE.MAX_CHARGING"),
                controlName: "forceChargeMinPower",
                converter: Converter.POWER_IN_WATT,
                hide: (el) => IS_NOT_FORCE_CHARGE(el) || ctrl == null,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "forceChargeMinPower",
                properties: {
                    tickMin: minChargePower,
                    tickMax: maxChargePower,
                    step: 100,
                    unit: "W",
                    pinFormatter: (val: number) => Converter.POWER_IN_WATT(val),
                },
                hide: (el) => IS_NOT_FORCE_CHARGE(el) || ctrl == null,
            },
            // ── Excess power settings ──────────────────────────────────
            {
                type: "toggle-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.MIN_CHARGING"),
                controlName: "minGuarantee",
                hide: (el) => IS_NOT_EXCESS_POWER(el) || ctrl == null,
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.MIN_CHARGE_POWER"),
                controlName: "defaultChargeMinPower",
                converter: Converter.POWER_IN_WATT,
                hide: (el) => IS_NO_MIN_GUARANTEE(el) || ctrl == null,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "defaultChargeMinPower",
                properties: {
                    tickMin: minChargePower,
                    tickMax: maxChargePower,
                    step: 100,
                    unit: "W",
                    pinFormatter: (val: number) => Converter.POWER_IN_WATT(val),
                },
                hide: (el) => IS_NO_MIN_GUARANTEE(el) || ctrl == null,
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.PRIORITIZATION"),
                controlName: "priority",
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.CHARGING_PRIORITY.CAR"),
                        value: "CAR",
                        icon: {
                            color: "success",
                            name: "oe-evcs",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant(
                            "EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.CHARGING_PRIORITY.STORAGE",
                        ),
                        value: "STORAGE",
                        icon: {
                            color: "success",
                            name: "oe-storage",
                            size: "medium",
                        },
                    },
                ],
                hide: (el) => IS_NOT_EXCESS_POWER(el) || ctrl == null,
            },
            { type: "horizontal-line" },
            // ── Energy limit settings ──────────────────────────────────
            {
                type: "toggle-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.MAX_ENERGY_RESTRICTION"),
                controlName: "energyLimit",
                hide: (el) => IS_OFF_OR_NULL(el) || ctrl == null,
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.ENERGY_LIMIT"),
                controlName: "energySessionLimit",
                converter: Converter.TO_KILO_WATT_HOURS,
                hide: (el) => IS_NO_ENERGY_LIMIT(el) || ctrl == null,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "energySessionLimitKwh",
                properties: {
                    tickMin: 1,
                    tickMax: 100,
                    step: 1,
                    unit: "kWh ",
                    pinFormatter: (val: number) => `${val} kWh`,
                },
                hide: (el) => IS_NO_ENERGY_LIMIT(el) || ctrl == null,
            },
        ];

        return {
            title: component.alias,
            icon: { name: "oe-evcs", color: "normal", size: "large" },
            lines,
            component: ctrl ?? component,
        };
    }

    protected override generateView(): OeFormlyView<EvcsSettingsViewModel> {
        const component = this.getComponent();
        const ctrl = this.getCtrlSafe();
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        return {
            ...EvcsSettingsComponent.generateView(
                this.translate,
                component,
                ctrl,
                this.minChargePower,
                this.maxChargePower,
            ),
            edge,
        };
    }

    protected override getFormGroup(): FormGroup {
        const ctrl = this.getCtrlSafe();
        const DEFAULT_ENERGY_SESSION_LIMIT_KWH = 20;

        const rawEnergySessionLimit: number | null =
            ctrl?.getPropertyFromComponent<number>("energySessionLimit") ?? null;
        const energySessionLimitKwh =
            rawEnergySessionLimit != null && rawEnergySessionLimit > 0
                ? Math.round(rawEnergySessionLimit / 1000)
                : DEFAULT_ENERGY_SESSION_LIMIT_KWH;

        return new FormGroup({
            chargeMode: new FormControl(
                ctrl?.getPropertyFromComponent<boolean>("enabledCharging") === false
                    ? "OFF"
                    : (ctrl?.getPropertyFromComponent<string>("chargeMode") ?? null),
            ),
            energyLimit: new FormControl(rawEnergySessionLimit == null ? null : rawEnergySessionLimit > 0),
            minGuarantee: new FormControl(
                ctrl?.getPropertyFromComponent<number>("defaultChargeMinPower") == null
                    ? null
                    : (ctrl?.getPropertyFromComponent<number>("defaultChargeMinPower") ?? 0) > 0,
            ),
            defaultChargeMinPower: new FormControl(
                ctrl?.getPropertyFromComponent<number>("defaultChargeMinPower") ?? null,
            ),
            forceChargeMinPower: new FormControl(ctrl?.getPropertyFromComponent<number>("forceChargeMinPower") ?? null),
            priority: new FormControl(ctrl?.getPropertyFromComponent<string>("priority") ?? null),
            energySessionLimit: new FormControl(rawEnergySessionLimit),
            energySessionLimitKwh: new FormControl(energySessionLimitKwh),
            enabledCharging: new FormControl(null),
            isReadWrite: new FormControl(null),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        const ctrl = this.getCtrlSafe();
        if (ctrl == null) {
            return SharedEvcs.getChannelAddresses(component, null);
        }
        return SharedEvcs.getChannelAddresses(component, ctrl);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.getComponent();
        const ctrl = this.getCtrlSafe();

        AssertionUtils.assertIsDefined(component);

        this.numberOfPhases = currentData.allComponents[component.id + "/Phases"] ?? 3;

        // Extract and store hardware power limits for reference
        // Hardware limits are per-phase, so multiply by numberOfPhases
        const rawMinChargePower = currentData.allComponents[component.id + "/MinimumHardwarePower"];
        const rawMaxChargePower = currentData.allComponents[component.id + "/MaximumHardwarePower"];

        if (rawMinChargePower != null) {
            this.minChargePower = Math.ceil(rawMinChargePower / 100) * 100;
        }
        if (rawMaxChargePower != null) {
            this.maxChargePower = Math.ceil(rawMaxChargePower / 100) * 100;
        }

        // isReadWrite is derived from component config, not a live channel
        const isReadWrite = component.getPropertyFromComponent<boolean>("readOnly") !== true;
        this.setFormControlSafelyWithValue(this.form(), "isReadWrite", isReadWrite);

        if (ctrl == null) {
            return;
        }

        if (!this.form().dirty) {
            const enabledCharging = currentData.allComponents[ctrl.id + "/_PropertyEnabledCharging"];
            const rawChargeMode = currentData.allComponents[ctrl.id + "/_PropertyChargeMode"];
            const chargeMode = enabledCharging === 1 ? rawChargeMode : "OFF";
            this.setFormControlSafelyWithValue(this.form(), "chargeMode", chargeMode);
        }

        this.setFormControlSafelyWithChannel(
            this.form(),
            "forceChargeMinPower",
            currentData,
            new ChannelAddress(ctrl.id, "_PropertyForceChargeMinPower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form(),
            "defaultChargeMinPower",
            currentData,
            new ChannelAddress(ctrl.id, "_PropertyDefaultChargeMinPower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form(),
            "priority",
            currentData,
            new ChannelAddress(ctrl.id, "_PropertyPriority"),
        );
        this.setFormControlSafelyWithChannel(
            this.form(),
            "energySessionLimit",
            currentData,
            new ChannelAddress(ctrl.id, "_PropertyEnergySessionLimit"),
        );

        // Derived UI-only controls – only update when form is pristine
        if (!this.form().dirty) {
            const defaultChargeMinPower = currentData.allComponents[ctrl.id + "/_PropertyDefaultChargeMinPower"];
            this.setFormControlSafelyWithValue(
                this.form(),
                "minGuarantee",
                defaultChargeMinPower == null ? null : defaultChargeMinPower > 0,
            );

            const energySessionLimit = currentData.allComponents[ctrl.id + "/_PropertyEnergySessionLimit"];
            this.setFormControlSafelyWithValue(
                this.form(),
                "energyLimit",
                energySessionLimit == null ? null : energySessionLimit > 0,
            );
            this.setFormControlSafelyWithValue(
                this.form(),
                "energySessionLimitKwh",
                energySessionLimit != null && energySessionLimit > 0 ? Math.round(energySessionLimit / 1000) : null,
            );
        }
    }

    protected override applyChanges(
        fg: FormGroup,
        service: Service,
        websocket: Websocket,
        _component: EdgeConfig.Component | null,
        _edge: Edge | null,
    ): void {
        const ctrl = this.getCtrlSafe();
        const edge = this.service.currentEdge();
        if (ctrl == null) {
            return;
        }
        AssertionUtils.assertIsDefined(edge);

        const changes: { name: string; value: unknown }[] = [];

        // chargeMode maps to both chargeMode and enabledCharging on the controller
        if (fg.controls["chargeMode"]?.dirty) {
            const chargeMode = fg.value["chargeMode"] as "FORCE_CHARGE" | "EXCESS_POWER" | "OFF";
            if (chargeMode === "OFF") {
                changes.push({ name: "enabledCharging", value: false });
            } else {
                changes.push({ name: "enabledCharging", value: true }, { name: "chargeMode", value: chargeMode });
            }
        }

        // minGuarantee toggle drives defaultChargeMinPower
        if (fg.controls["minGuarantee"]?.dirty) {
            changes.push({
                name: "defaultChargeMinPower",
                value: fg.value["minGuarantee"]
                    ? (fg.controls["defaultChargeMinPower"]?.value ?? 1400 * this.numberOfPhases)
                    : 0,
            });
        } else if (fg.controls["defaultChargeMinPower"]?.dirty) {
            changes.push({
                name: "defaultChargeMinPower",
                value: fg.value["defaultChargeMinPower"],
            });
        }

        if (fg.controls["forceChargeMinPower"]?.dirty) {
            changes.push({
                name: "forceChargeMinPower",
                value: fg.value["forceChargeMinPower"],
            });
        }

        if (fg.controls["priority"]?.dirty) {
            changes.push({ name: "priority", value: fg.value["priority"] });
        }

        // energyLimit toggle zeroes out energySessionLimit
        if (fg.controls["energyLimit"]?.dirty && !fg.value["energyLimit"]) {
            changes.push({ name: "energySessionLimit", value: 0 });
        }

        // energySessionLimitKwh range drives energySessionLimit (Wh)
        if (fg.controls["energySessionLimitKwh"]?.dirty) {
            changes.push({
                name: "energySessionLimit",
                value: (fg.value["energySessionLimitKwh"] as number) * 1000,
            });
        }

        if (changes.length === 0) {
            return;
        }

        service.startSpinner("formly-field-modal");
        edge.updateComponentConfig(websocket, ctrl.id, changes)
            .then(() => {
                service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
            })
            .catch((reason) => {
                service.toast(this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + reason.error.message, "danger");
            })
            .finally(() => {
                this.skipCurrentData = true;
                fg.markAsPristine();
                service.stopSpinner("formly-field-modal");
            });
    }

    protected override getComponent(): EdgeConfig.Component {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);
        return component;
    }

    private getCtrlSafe(): EdgeConfig.Component | null {
        const edge = this.service.currentEdge();
        if (!edge) {
            return null;
        }
        const config = edge.getCurrentConfig();
        if (!config) {
            return null;
        }
        const componentId = this.routeService.getRouteParam("componentId");
        if (!componentId) {
            return null;
        }
        const controllers = config.getComponentsByFactory("Controller.Evcs");
        return controllers.find((c) => c.getPropertyFromComponent<string>("evcs.id") === componentId) ?? null;
    }
}
