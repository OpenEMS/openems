import { Component } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { LiveDataService } from "../../../livedataservice";
import { SharedSelfConsumption } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class CommonSelfConsumptionHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(translate: TranslateService): OeFormlyView {
        return SharedSelfConsumption.getFormlyView(translate);
    }

    protected override generateView(): OeFormlyView {
        return CommonSelfConsumptionHomeComponent.generateView(this.translate);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedSelfConsumption.getChannelAddresses();
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithValue(this.form, "selfConsumption", SharedSelfConsumption.getSelfConsumptionValue(currentData));
    }

    protected override getFormGroup(): FormGroup {
        return SharedSelfConsumption.getFormGroup();
    }
}
