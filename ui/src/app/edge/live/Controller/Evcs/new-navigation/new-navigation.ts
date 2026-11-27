import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedEvcs } from "../shared/shared";

@Component({
    selector: "oe-evcs-home",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,

    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class EvcsHomeComponent extends AbstractFormlyComponent<SharedEvcs.EvcsViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;
    private ctrl: EdgeConfig.Component | null = null;

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        ctrl: EdgeConfig.Component | null,
    ): OeFormlyView<SharedEvcs.EvcsViewModel> {
        const HIDE_WHEN_HAS_CONNECTION = (el: { hasConnection: boolean }) => el.hasConnection;
        const HIDE_WHEN_ENERGY_SINCE_BEGINNING_ALLOWED = (el: { isEnergySinceBeginningAllowed: boolean }) =>
            el.isEnergySinceBeginningAllowed;
        const HIDE_WHEN_READ_ONLY_AND_CHARGING_ENABLED = (el: { isReadOnly: boolean; isChargingEnabled: boolean }) =>
            el.isReadOnly && el.isChargingEnabled;
        const HIDE_WHEN_NO_DEFAULT_CHARGE_MIN_POWER_OR_CHARGING_ENABLED_OR_NOT_EXCESS_POWER = (el: {
            hasNoDefaultChargeMinPower: boolean;
            isNotExcessPower: boolean;
            isChargingEnabled: boolean;
        }) => {
            return el.hasNoDefaultChargeMinPower || el.isChargingEnabled || el.isNotExcessPower;
        };
        const HIDE_WHEN_CHARGING_ENABLED_OR_NOT_EXCESS_POWER = (el: {
            isNotExcessPower: boolean;
            isChargingEnabled: boolean;
        }) => el.isChargingEnabled || el.isNotExcessPower;
        const HIDE_WHEN_CHARGING_ENABLED_OR_NOT_FORCE_CHARGE = (el: {
            isChargingEnabled: boolean;
            isNotForceCharge: boolean;
        }) => el.isChargingEnabled || el.isNotForceCharge;
        const HIDE_WHEN_NO_ENERGY_SESSION_LIMIT_OR_CHARGING_ENABLED = (el: {
            hasNoEnergySessionLimit: boolean;
            isChargingEnabled: boolean;
        }) => el.hasNoEnergySessionLimit || el.isChargingEnabled;

        const commonLines: OeFormlyField<SharedEvcs.EvcsViewModel>[] = [
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.NO_CONNECTION.DESCRIPTION"),
                hide: HIDE_WHEN_HAS_CONNECTION,
                cssClass: "ion-text-font-style-italic",
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.STATUS"),
                channel: component.id + "/State",
                converter: CONVERT_EVCS_STATUS(translate),
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING_POWER"),
                channel: component.id + "/ActivePower",
                converter: Converter.POWER_IN_WATT,
                filter: (value: number | null) => value != null && value !== 0,
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGE_TARGET"),
                channel: component.id + "/SetChargePowerLimit",
                converter: Converter.POWER_IN_WATT,
                hide: (el) => el.isReadOnly && ctrl == null,
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.ENERGY_SINCE_BEGINNING"),
                channel: component.id + "/EnergySession",
                converter: Converter.CONVERT_TO_WATTHOURS(),
                hide: HIDE_WHEN_ENERGY_SINCE_BEGINNING_ALLOWED,
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.UNCONTROLLABLE"),
                hide: (el) => !(el.isReadOnly && ctrl == null),
            },
        ];

        if (ctrl == null) {
            return {
                title: component.alias,
                icon: { name: "oe-evcs", color: "normal", size: "large" },
                lines: commonLines,
                component,
            };
        }
        const controlledLines: OeFormlyField<SharedEvcs.EvcsViewModel>[] = [
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MODE"),
                channel: ctrl.id + "/_PropertyChargeMode",
                converter: CONVERT_CHARGE_MODE(translate),
                hide: HIDE_WHEN_READ_ONLY_AND_CHARGING_ENABLED,
            },
            {
                hide: HIDE_WHEN_NO_DEFAULT_CHARGE_MIN_POWER_OR_CHARGING_ENABLED_OR_NOT_EXCESS_POWER,
                type: "info-line",
                name:
                    translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.MIN_CHARGING") +
                    ": " +
                    translate.instant("GENERAL.ACTIVE"),
            },
            {
                hide: HIDE_WHEN_NO_DEFAULT_CHARGE_MIN_POWER_OR_CHARGING_ENABLED_OR_NOT_EXCESS_POWER,
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.MIN_CHARGE_POWER"),
                converter: Converter.POWER_IN_WATT,
                channel: ctrl.id + "/_PropertyDefaultChargeMinPower",
            },
            {
                hide: HIDE_WHEN_CHARGING_ENABLED_OR_NOT_EXCESS_POWER,
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.PRIORITIZATION"),
                channel: ctrl.id + "/_PropertyPriority",
                converter: CONVERT_PRIORITIZATION(translate),
            },
            {
                hide: HIDE_WHEN_CHARGING_ENABLED_OR_NOT_FORCE_CHARGE,
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_CHARGE_MODE.MAX_CHARGING"),
                channel: ctrl.id + "/_PropertyForceChargeMinPower",
                converter: Converter.POWER_IN_KILO_WATT,
            },
            {
                hide: HIDE_WHEN_NO_ENERGY_SESSION_LIMIT_OR_CHARGING_ENABLED,
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.ENERGY_LIMIT"),
                channel: ctrl.id + "/_PropertyEnergySessionLimit",
                converter: Converter.TO_KILO_WATT_HOURS,
            },
        ];

        const lines: OeFormlyField<SharedEvcs.EvcsViewModel>[] = [...commonLines, ...controlledLines];

        return {
            title: component.alias,
            icon: { name: "oe-evcs", color: "normal", size: "large" },
            lines: lines,
            component,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        AssertionUtils.assertIsDefined(this.component);
        const component = this.component;
        this.setFormControlSafelyWithValue(
            this.form(),
            "isEnergySinceBeginningAllowed",
            !(
                currentData.allComponents[component.id + "/ActivePower"] > 0 ||
                currentData.allComponents[component.id + "/Status"] == 2 ||
                currentData.allComponents[component.id + "/Status"] == 7
            ),
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "hasConnection",
            currentData.allComponents[component.id + "/State"] !== 3,
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "isReadOnly",
            component.getPropertyFromComponent<boolean>("readOnly") === true,
        );

        if (this.ctrl == null) {
            return;
        }
        const ctrl = this.ctrl;

        this.setFormControlSafelyWithValue(
            this.form(),
            "isChargingEnabled",
            currentData.allComponents[ctrl.id + "/_PropertyEnabledCharging"] === 1,
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "isNotExcessPower",
            ctrl.getPropertyFromComponent<string>("chargeMode") !== "EXCESS_POWER",
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "isNotForceCharge",
            ctrl.getPropertyFromComponent<string>("chargeMode") !== "FORCE_CHARGE",
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "isReadOnly",
            component.getPropertyFromComponent<boolean>("readOnly") === true,
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "hasNoDefaultChargeMinPower",
            (ctrl.getPropertyFromComponent<number>("defaultChargeMinPower") ?? 0) <= 0,
        );
        this.setFormControlSafelyWithValue(
            this.form(),
            "hasNoEnergySessionLimit",
            (ctrl.getPropertyFromComponent<number>("energySessionLimit") ?? 0) <= 0,
        );
    }

    protected override generateView(): OeFormlyView<SharedEvcs.EvcsViewModel> {
        this.setComponentAndCtrl();
        AssertionUtils.assertIsDefined(this.component);
        const component = this.component;

        return EvcsHomeComponent.generateView(this.translate, component, this.ctrl);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        this.setComponentAndCtrl();
        AssertionUtils.assertIsDefined(this.component);
        const component = this.component;
        return SharedEvcs.getChannelAddresses(component, this.ctrl);
    }

    protected override getFormGroup(): FormGroup<{}> {
        return SharedEvcs.getFormGroup();
    }

    private setComponentAndCtrl() {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);

        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);
        const component = this.component;

        const controllers = config.getComponentsByFactory("Controller.Evcs");

        this.ctrl =
            controllers.find((controller) => controller.getPropertyFromComponent<string>("evcs.id") === component.id) ??
            null;
    }
}

export const CONVERT_CHARGE_MODE = (translate: TranslateService): Converter => {
    return (value: number | string | null): string => {
        if (value == null) {
            return "-";
        }

        const strValue = value.toString();
        switch (value) {
            case "FORCE_CHARGE":
                return translate.instant("GENERAL.MANUALLY");

            case "EXCESS_POWER":
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.SHORT_NAME");

            case "OFF":
                return translate.instant("GENERAL.OFF");

            default:
                return strValue;
        }
    };
};

export const CONVERT_PRIORITIZATION = (translate: TranslateService): Converter => {
    return (value: number | string | null): string => {
        if (value == null) {
            return "-";
        }

        const strValue = value.toString();

        return translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.CHARGING_PRIORITY." + strValue);
    };
};

export const CONVERT_EVCS_STATUS = (translate: TranslateService): ((value: number | null) => string) => {
    return (value: number | null): string => {
        if (value == null) {
            return "-";
        }

        switch (value) {
            case 0:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.STARTING");
            case -1:
            case 4:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.ERROR");

            case 2:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");

            case 1:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");

            case 5:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");

            case 3:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING");

            case 6:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");

            case 7:
                return translate.instant("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");

            default:
                return "-";
        }
    };
};
