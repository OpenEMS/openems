import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoFixDigitalOutput } from "../shared/shared";

@Component({
    selector: "oe-controller-io-fix-digital-output",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerFixDigitalOutputHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private component: EdgeConfig.Component | null = null;

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);
        this.component = component;

        return SharedControllerIoFixDigitalOutput.getFormlyView(this.translate, component, edge);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerIoFixDigitalOutput.getChannelAddresses(this.service, this.routeService, this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const fixDigitalOutputComponent = this.component;

        AssertionUtils.assertIsDefined(fixDigitalOutputComponent);
        fixDigitalOutputComponent.getPropertyFromComponent("outputChannelAddress");

        this.setFormControlSafelyWithChannel(
            this.form,
            "isOn",
            currentData,
            new ChannelAddress(fixDigitalOutputComponent.id, "_PropertyIsOn"),
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoFixDigitalOutput.getFormGroup();
    }
}
