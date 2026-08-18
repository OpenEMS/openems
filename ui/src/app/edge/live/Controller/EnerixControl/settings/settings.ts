import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedControllerEnerixControl } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerEnerixControlSettingsComponent extends AbstractFormlyComponent<SharedControllerEnerixControl.EnerixControlViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private component: EdgeConfig.Component | null = null;

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<SharedControllerEnerixControl.EnerixControlViewModel> {
        return SharedControllerEnerixControl.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<SharedControllerEnerixControl.EnerixControlViewModel> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);

        return ControllerEnerixControlSettingsComponent.generateView(this.translate, component, edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerEnerixControl.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);
        return SharedControllerEnerixControl.getChannelAddresses(this.service, this.routeService, component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.component ??= this.getComponent();

        this.setFormControlSafelyWithChannel(
            this.form,
            "controlMode",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyControlMode"),
        );
    }
}
