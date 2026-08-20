import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";

/** Empty page for navigation purposes */
@Component({
    selector: "oe-empty-page",
    templateUrl: "./empty-page.html",
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class EmptyPageComponent {}
