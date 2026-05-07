import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { HeatingElementViewModel, SharedControllerIoHeatingElement } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class ControllerIoHeatingElementSettingsComponent extends AbstractFormlyComponent<HeatingElementViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView<HeatingElementViewModel> {
        return SharedControllerIoHeatingElement.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<HeatingElementViewModel> {
        const component = this.getComponent();
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        return ControllerIoHeatingElementSettingsComponent.generateView(this.translate, component, edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoHeatingElement.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedControllerIoHeatingElement.getChannelAddresses(component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.getComponent();

        AssertionUtils.assertIsDefined(component);
        this.setFormControlSafelyWithChannel(this.form, "mode", currentData, new ChannelAddress(component.id, "_PropertyMode"));
        this.setFormControlSafelyWithChannel(this.form, "defaultLevel", currentData, new ChannelAddress(component.id, "_PropertyDefaultLevel"));
        this.setFormControlSafelyWithChannel(this.form, "minTime", currentData, new ChannelAddress(component.id, "_PropertyMinTime"));
        this.setFormControlSafelyWithChannel(this.form, "workMode", currentData, new ChannelAddress(component.id, "_PropertyWorkMode"));
        this.setFormControlSafelyWithChannel(this.form, "endTime", currentData, new ChannelAddress(component.id, "_PropertyEndTime"));
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
