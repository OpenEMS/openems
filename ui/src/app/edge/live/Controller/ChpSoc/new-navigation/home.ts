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
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { ControllerChpFlatComponent } from "../flat/ChpSoc";
import { ChpViewModel, SharedControllerChpSoc } from "../shared/shared";
import { CommonChpPercentagebarComponent } from "./percentagebar/percentagebar";

@Component({
    selector: "oe-controller-chp-home",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerChpHomeComponent extends AbstractFormlyComponent<ChpViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<ChpViewModel> {
        const lines: OeFormlyField<ChpViewModel>[] = [];

        const outputChannelAddress = component.getPropertyFromComponent<string>("outputChannelAddress");
        AssertionUtils.assertIsDefined(outputChannelAddress);

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MODE"),
                channel: component.id + ControllerChpFlatComponent.PROPERTY_MODE,
                converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: outputChannelAddress,
                converter: SharedControllerChpSoc.CONVERT_CHP_STATE(translate),
            },
            {
                type: "component-line",
                component: CommonChpPercentagebarComponent,
                hide: (el) => el.mode !== Mode.AUTOMATIC,
            },
        );

        return {
            title: component.alias,
            icon: { name: "flame-outline", color: "normal", size: "large" },
            helpKey: "REDIRECT.CONTROLLER_CHP_SOC",
            lines: lines,
            component: component,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            this.component = this.getComponent();
        }

        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(this.component.id, ControllerChpFlatComponent.PROPERTY_MODE),
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerChpSoc.getFormGroup();
    }

    protected override generateView(): OeFormlyView<ChpViewModel> {
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);
        return ControllerChpHomeComponent.generateView(this.translate, component);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedControllerChpSoc.getChannelAddresses(component);
    }
}
