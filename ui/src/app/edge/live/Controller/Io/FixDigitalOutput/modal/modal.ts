import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoFixDigitalOutput } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerFixDigitalOutputModalComponent extends AbstractFormlyComponent {
    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public edge: Edge | null = null;

    public static generateView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView {
        return SharedControllerIoFixDigitalOutput.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView {
        AssertionUtils.assertIsDefined(this.component);
        AssertionUtils.assertIsDefined(this.edge);
        return ControllerFixDigitalOutputModalComponent.generateView(this.translate, this.component, this.edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoFixDigitalOutput.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerIoFixDigitalOutput.getChannelAddresses(this.service, this.routeService, this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        this.setFormControlSafelyWithChannel(
            this.form,
            "isOn",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyIsOn"),
        );
    }
}
