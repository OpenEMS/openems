import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
})
export class ModalComponent extends AbstractFormlyComponent {

    public static generateView(translate: TranslateService): OeFormlyView {
        return {
            title: translate.instant("General.selfConsumption"),
            lines: [{
                type: "info-line",
                name: translate.instant("Edge.Index.Widgets.selfconsumptionInfo"),
            }],
        };
    }
    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return ModalComponent.generateView(this.translate);
    }

}
