import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { LiveDataService } from "../../../../livedataservice";
import { AutomaticViewModel, SharedControllerEssTimeOfUseTariff } from "../shared/shared";
import { Controller_Ess_TimeOfUseTariffUtils } from "../utils";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerEssTimeOfUseTariffSettingsComponent extends AbstractFormlyComponent<AutomaticViewModel> {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private lastSelectedMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode | null = null;
    private component: EdgeConfig.Component | null = null;

    private routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge, service: Service): OeFormlyView<AutomaticViewModel> {
        return SharedControllerEssTimeOfUseTariff.getFormlyView(translate, component, edge, service);
    }

    protected override generateView(): OeFormlyView<AutomaticViewModel> {
        const edge = this.service.currentEdge();
        this.component = this.getComponent();
        return ControllerEssTimeOfUseTariffSettingsComponent.generateView(this.translate, this.component, edge, this.service);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerEssTimeOfUseTariff.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        if (this.component == null) {
            this.component = this.getComponent();
        }
        return SharedControllerEssTimeOfUseTariff.getChannelAddresses(this.service, this.routeService, this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            this.component = this.getComponent();
        }

        this.setFormControlSafelyWithChannel(this.form, "mode", currentData, new ChannelAddress(this.component.id, "_PropertyMode"));
        this.setFormControlSafelyWithChannel(this.form, "controlMode", currentData, new ChannelAddress(this.component.id, "_PropertyControlMode"));

        if (this.lastSelectedMode === currentData.allComponents[this.component.id + "/_PropertyControlMode"]) {
            return;
        }

        this.setFormControlSafelyWithValue(this.form, "chargeConsumptionIsActive", currentData.allComponents[this.component.id + "/_PropertyControlMode"] === Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION ? true : false);
        this.lastSelectedMode = currentData.allComponents[this.component.id + "/_PropertyControlMode"];
        this.subscribeToggle(this.component, this.form);
    }

    private subscribeToggle(component: EdgeConfig.Component, fg: FormGroup<any>) {
        FormUtils.findFormControlSafely(fg, "chargeConsumptionIsActive")?.valueChanges
            .pipe()
            .subscribe((chargeConsumptionIsActive: boolean) => {
                const lastControlMode = component.properties["controlMode"];
                const controlMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode = chargeConsumptionIsActive
                    ? Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION
                    : Controller_Ess_TimeOfUseTariffUtils.ControlMode.DELAY_DISCHARGE;

                this.form.controls["controlMode"].setValue(controlMode);

                if (controlMode != lastControlMode) {
                    this.form.controls["controlMode"].markAsDirty();
                }
            });
    }

    private getComponent(): EdgeConfig.Component {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        return component;
    }
}
