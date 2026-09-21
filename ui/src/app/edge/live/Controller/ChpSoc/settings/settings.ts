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
import { ControllerChpFlatComponent } from "../flat/ChpSoc";
import { ChpViewModel, SharedControllerChpSoc } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerChpSettingsComponent extends AbstractFormlyComponent<ChpViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<ChpViewModel> {
        return SharedControllerChpSoc.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<ChpViewModel> {
        const component = this.getComponent();
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        return ControllerChpSettingsComponent.generateView(this.translate, component, edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerChpSoc.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedControllerChpSoc.getChannelAddresses(component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.getComponent();

        AssertionUtils.assertIsDefined(component);
        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(component.id, ControllerChpFlatComponent.PROPERTY_MODE),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "lowThreshold",
            currentData,
            new ChannelAddress(component.id, "_PropertyLowThreshold"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "highThreshold",
            currentData,
            new ChannelAddress(component.id, "_PropertyHighThreshold"),
        );
    }
}
