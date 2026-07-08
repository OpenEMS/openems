import { Component } from "@angular/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView, ViewContext } from "src/app/shared/components/shared/oe-formly-component";
import { LiveDataService } from "../../../livedataservice";
import { SharedGrid } from "../shared/shared";

@Component({
    selector: "oe-common-grid-modal",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ModalComponent extends AbstractFormlyComponent {

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        return SharedGrid.getFormlyView(viewContext.config, viewContext.edge.role, this.translate);
    }
}
