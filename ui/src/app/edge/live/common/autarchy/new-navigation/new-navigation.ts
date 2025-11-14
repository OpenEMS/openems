import { Component } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { LiveDataService } from "../../../livedataservice";
import { SharedAutarchy } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class CommonAutarchyHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(translate: TranslateService): OeFormlyView {
        return SharedAutarchy.getFormlyView(translate);
    }

    protected override generateView(): OeFormlyView {
        return CommonAutarchyHomeComponent.generateView(this.translate);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedAutarchy.getChannelAddresses();
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithValue(this.form, "autarchy", SharedAutarchy.getAutarchyValue(currentData));
    }

    protected override getFormGroup(): FormGroup {
        return SharedAutarchy.getFormGroup();
    }
}
