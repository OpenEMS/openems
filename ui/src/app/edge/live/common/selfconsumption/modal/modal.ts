import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { LiveDataService } from "../../../livedataservice";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ModalComponent extends AbstractFormlyComponent {

    public static generateView(translate: TranslateService): OeFormlyView {
        return {
            title: translate.instant("GENERAL.SELF_CONSUMPTION"),
            helpKey: "REDIRECT.COMMON_SELFCONSUMPTION",
            lines: [{
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SELFCONSUMPTION_INFO"),
            }],
        };
    }
    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return ModalComponent.generateView(this.translate);
    }

}
