import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket, } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedEssFixDigitalPowerControl } from "../shared/shared";

type FormModel = SharedEssFixDigitalPowerControl.FormModel;
type PowerDirection = SharedEssFixDigitalPowerControl.PowerDirection;

@Component({
    selector: "oe-controller-ess-fix-active-power-home",
    templateUrl:
        "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerEssFixActivePowerHomeComponent extends AbstractFormlyComponent<FormModel> {
    public component: EdgeConfig.Component | null = null;

    protected override formlyWrapper:
        | "formly-field-modal"
        | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    protected override generateView(): OeFormlyView<FormModel> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        this.component = config.getComponentSafely(
            this.routeService.getRouteParam("componentId"),
        );
        AssertionUtils.assertIsDefined(this.component);

        return SharedEssFixDigitalPowerControl.getFormlyView(
            this.translate,
            this.component,
            edge,
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedEssFixDigitalPowerControl.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedEssFixDigitalPowerControl.getChannelAddresses(
            this.service,
            this.routeService,
            this.component,
        );
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.component;
        AssertionUtils.assertIsDefined(component);

        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(
                component.id,
                SharedEssFixDigitalPowerControl.PROPERTY_MODE,
            ),
        );

        if (this.skipCurrentData || this.form.dirty || this.form.touched) {
            return;
        }

        const powerChannel = new ChannelAddress(
            component.id,
            SharedEssFixDigitalPowerControl.PROPERTY_POWER,
        );
        const signedPower: number | null =
            currentData.allComponents[powerChannel.toString()] ?? null;
        if (signedPower == null) {
            return;
        }

        const direction: PowerDirection =
            signedPower < 0 ? "CHARGE" : "DISCHARGE";
        this.setFormControlSafelyWithValue(
            this.form,
            "powerDirection",
            direction,
        );
        this.setFormControlSafelyWithValue(
            this.form,
            "power",
            Math.abs(signedPower),
        );

        this.setFormControlSafelyWithChannel(
            this.form,
            "chargeOncePower",
            currentData,
            new ChannelAddress(
                component.id,
                SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_POWER,
            ),
        );

        const chargeOnceTargetSocEnableRaw: number | null =
            currentData.allComponents[
                new ChannelAddress(
                    component.id,
                    SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_TARGET_SOC_ENABLE,
                ).toString()
            ] ?? null;
        this.setFormControlSafelyWithValue(
            this.form,
            "chargeOnceTargetSocEnable",
            chargeOnceTargetSocEnableRaw != null
                ? chargeOnceTargetSocEnableRaw === 1
                : null,
        );

        this.setFormControlSafelyWithChannel(
            this.form,
            "chargeOnceTargetSoc",
            currentData,
            new ChannelAddress(
                component.id,
                SharedEssFixDigitalPowerControl.PROPERTY_CHARGE_ONCE_TARGET_SOC,
            ),
        );

        this.setFormControlSafelyWithChannel(
            this.form,
            "dischargeOncePower",
            currentData,
            new ChannelAddress(
                component.id,
                SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_POWER,
            ),
        );

        const dischargeOnceTargetSocEnableRaw: number | null =
            currentData.allComponents[
                new ChannelAddress(
                    component.id,
                    SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_TARGET_SOC_ENABLE,
                ).toString()
            ] ?? null;
        this.setFormControlSafelyWithValue(
            this.form,
            "dischargeOnceTargetSocEnable",
            dischargeOnceTargetSocEnableRaw != null
                ? dischargeOnceTargetSocEnableRaw === 1
                : null,
        );

        this.setFormControlSafelyWithChannel(
            this.form,
            "dischargeOnceTargetSoc",
            currentData,
            new ChannelAddress(
                component.id,
                SharedEssFixDigitalPowerControl.PROPERTY_DISCHARGE_ONCE_TARGET_SOC,
            ),
        );

        const meta = new MetaComponent(
            this.service.currentEdge().getCurrentConfig(),
        );
        if (meta != null) {
            this.setFormControlSafelyWithChannel(
                this.form,
                "isEssChargeToGridAllowed",
                currentData,
                new ChannelAddress(
                    meta.id,
                    SharedEssFixDigitalPowerControl.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED,
                ),
            );

            this.setFormControlSafelyWithChannel(
                this.form,
                "isEssDischargeToGridAllowed",
                currentData,
                new ChannelAddress(
                    meta.id,
                    SharedEssFixDigitalPowerControl.CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED,
                ),
            );
        }
    }

    /**
     * Overrides the default update to handle the UI-only powerDirection
     * control:
     *
     * - Excludes powerDirection from the edge update
     * - Applies the correct sign to power: negative for charging, positive for
     *   discharging
     */
    protected override buildUpdateComponentArr(
        fg: FormGroup<any>,
    ): { name: string; value: any }[] {
        const updates = super
            .buildUpdateComponentArr(fg)
            .filter((update) => update.name !== "powerDirection")
            .filter((update) => update.name !== "isEssChargeToGridAllowed")
            .filter((update) => update.name !== "isEssDischargeToGridAllowed");

        const direction: PowerDirection =
            fg.value["powerDirection"] ?? "DISCHARGE";
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

    protected override applyChanges(
        fg: FormGroup<any>,
        service: Service,
        websocket: Websocket,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): void {
        super.applyChanges(fg, service, websocket, component, edge);

        const chargeConsumptionControl =
            fg.controls["isEssChargeToGridAllowed"];
        const dischargeToGridControl =
            fg.controls["isEssDischargeToGridAllowed"];

        const isChargeDirty = chargeConsumptionControl?.dirty ?? false;
        const isDischargeDirty = dischargeToGridControl?.dirty ?? false;

        if (!isChargeDirty && !isDischargeDirty) {
            return;
        }

        const config = edge?.getCurrentConfig() ?? null;
        const meta = new MetaComponent(config);
        if (meta == null || edge == null) {
            return;
        }

        const metaUpdates: { name: string; value: any }[] = [];
        if (isChargeDirty) {
            metaUpdates.push({
                name: "isEssChargeFromGridAllowed",
                value: chargeConsumptionControl.value ?? false,
            });
        }
        if (isDischargeDirty) {
            metaUpdates.push({
                name: "isEssDischargeToGridAllowed",
                value: dischargeToGridControl.value ?? false,
            });
        }

        edge.updateComponentConfig(websocket, meta.id, metaUpdates).catch(
            (reason) => {
                service.toast(
                    this.translate.instant("GENERAL.CHANGE_FAILED") +
                        "\n" +
                        reason.error.message,
                    "danger",
                );
            },
        );
    }
}
