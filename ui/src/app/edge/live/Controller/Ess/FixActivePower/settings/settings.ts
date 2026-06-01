import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedEssFixDigitalPowerControl } from "../shared/shared";

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
export class ControllerEssFixActivePowerSettingsComponent extends AbstractFormlyComponent<{ mode: Mode }> {

    public component: EdgeConfig.Component | null = null;

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView<{ mode: Mode }> {
        return SharedEssFixDigitalPowerControl.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<{ mode: Mode }> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(this.component);

        return ControllerEssFixActivePowerSettingsComponent.generateView(this.translate, this.component, edge);
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

        this.setFormControlSafelyWithChannel(this.form, "mode", currentData, new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_MODE));
        this.setFormControlSafelyWithChannel(this.form, "power", currentData, new ChannelAddress(component.id, SharedEssFixDigitalPowerControl.PROPERTY_POWER));
    }
}
