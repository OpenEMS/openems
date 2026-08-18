import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
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
export class ControllerEnerixControlHomeComponent extends AbstractFormlyComponent<SharedControllerEnerixControl.EnerixControlViewModel> {
    public component: EdgeConfig.Component | null = null;
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<SharedControllerEnerixControl.EnerixControlViewModel> {
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_CLEVER_PV",
            lines: SharedControllerEnerixControl.getFormlySharedModeAndStateLines(translate, component),
            component: component,
        };
    }

    protected override generateView(): OeFormlyView<SharedControllerEnerixControl.EnerixControlViewModel> {
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);

        return ControllerEnerixControlHomeComponent.generateView(this.translate, component);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(component);

        return SharedControllerEnerixControl.getChannelAddresses(this.service, this.routeService, component);
    }
}
