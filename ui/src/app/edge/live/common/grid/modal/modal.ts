import { Component } from "@angular/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { LiveDataService } from "../../../livedataservice";
import { SharedGrid } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ModalComponent extends AbstractFormlyComponent {

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return SharedGrid.getFormlyView(config, role, this.translate);
    }
}
