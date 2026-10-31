import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedEssFixDigitalPowerControl } from "./shared";

type FormModel = SharedEssFixDigitalPowerControl.FormModel;
type PowerDirection = SharedEssFixDigitalPowerControl.PowerDirection;

export abstract class FixPowerComponent extends AbstractFormlyComponent<FormModel> {
    public component: EdgeConfig.Component | null = null;

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected abstract readonly powerConverter: (value: number | null) => string;
    protected abstract readonly unit: string;

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        powerConverter: (value: number | null) => string,
    ): OeFormlyView {
        return {
            title: component.alias,
            icon: { name: "swap-vertical-outline", color: "normal", size: "large" },
            lines: SharedEssFixDigitalPowerControl.getFormlySharedModeAndStateLines(
                translate,
                component,
                powerConverter,
            ),
            component: component,
        };
    }

    protected override generateView(): OeFormlyView<FormModel> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);

        return SharedEssFixDigitalPowerControl.getFormlyView(
            this.translate,
            this.component,
            edge,
            this.unit,
            this.powerConverter,
            () => {
                this.navigationService.navigateAbsoluteById("grid-settings");
            },
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedEssFixDigitalPowerControl.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedEssFixDigitalPowerControl.getChannelAddresses(this.service, this.routeService, this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.component;
        AssertionUtils.assertIsDefined(component);

        this.setFormControlSafelyWithChannel(
            this.form(),
            "mode",
            currentData,
            new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_MODE),
        );

        this.setFormControlSafelyWithBooleanChannel(
            this.form(),
            "isEssChargeFromGridAllowed",
            currentData,
            new ChannelAddress(
                "_meta",
                SharedEssFixDigitalPowerControl.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED,
            ),
        );
        this.setFormControlSafelyWithBooleanChannel(
            this.form(),
            "isEssDischargeToGridAllowed",
            currentData,
            new ChannelAddress(
                "_meta",
                SharedEssFixDigitalPowerControl.CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED,
            ),
        );

        if (this.skipCurrentData || this.form().dirty || this.form().touched) {
            return;
        }

        const powerChannel = new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_POWER);
        const signedPower: number | null = currentData.allComponents[powerChannel.toString()] ?? null;
        if (signedPower == null) {
            return;
        }

        const direction: PowerDirection = signedPower < 0 ? "CHARGE" : "DISCHARGE";
        this.setFormControlSafelyWithValue(this.form(), "powerDirection", direction);
        this.setFormControlSafelyWithValue(this.form(), "power", Math.abs(signedPower));

        this.setFormControlSafelyWithChannel(
            this.form(),
            "chargeOncePower",
            currentData,
            new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_POWER),
        );

        const chargeOnceTargetSocEnableRaw: number | null =
            currentData.allComponents[
                new ChannelAddress(
                    component.id,
                    SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_TARGET_SOC_ENABLE,
                ).toString()
            ] ?? null;
        this.setFormControlSafelyWithValue(
            this.form(),
            "chargeOnceTargetSocEnable",
            chargeOnceTargetSocEnableRaw != null ? chargeOnceTargetSocEnableRaw === 1 : null,
        );

        this.setFormControlSafelyWithChannel(
            this.form(),
            "chargeOnceTargetSoc",
            currentData,
            new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_TARGET_SOC),
        );

        this.setFormControlSafelyWithChannel(
            this.form(),
            "dischargeOncePower",
            currentData,
            new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_POWER),
        );

        const dischargeOnceTargetSocEnableRaw: number | null =
            currentData.allComponents[
                new ChannelAddress(
                    component.id,
                    SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_TARGET_SOC_ENABLE,
                ).toString()
            ] ?? null;
        this.setFormControlSafelyWithValue(
            this.form(),
            "dischargeOnceTargetSocEnable",
            dischargeOnceTargetSocEnableRaw != null ? dischargeOnceTargetSocEnableRaw === 1 : null,
        );

        this.setFormControlSafelyWithChannel(
            this.form(),
            "dischargeOnceTargetSoc",
            currentData,
            new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_TARGET_SOC),
        );
    }

    /**
     * Overrides the default update to handle the UI-only powerDirection control:
     *
     * - Excludes powerDirection from the edge update
     * - Applies the correct sign to power: negative for charging, positive for discharging
     */
    protected override buildUpdateComponentArr(fg: FormGroup<any>): { name: string; value: any }[] {
        const updates = super
            .buildUpdateComponentArr(fg)
            .filter((update) => update.name !== "powerDirection")
            .filter((update) => update.name !== "isEssChargeFromGridAllowed")
            .filter((update) => update.name !== "isEssDischargeToGridAllowed");

        const direction: PowerDirection = fg.value["powerDirection"] ?? "DISCHARGE";
        const isDirectionDirty = fg.controls["powerDirection"]?.dirty ?? false;

        // If direction changed but power was untouched, force-include the
        // existing power value with the correct new sign so the edge updates.
        if (isDirectionDirty || updates.some((u) => u.name === "power")) {
            const absValue: number = Math.abs(fg.value["power"] ?? 0);
            const signedValue = direction === "CHARGE" ? -absValue : absValue;

            const existingPowerUpdate = updates.find((u) => u.name === "power");
            if (existingPowerUpdate != null) {
                existingPowerUpdate.value = signedValue;
            } else {
                updates.push({ name: "power", value: signedValue });
            }
        }

        return updates;
    }
}
