import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { OeCheckboxComponent } from "src/app/shared/components/oe-checkbox/oe-checkbox";
import { SharedModule } from "src/app/shared/shared.module";
import { RegistrationModalComponent } from "./modal/modal.component";
import { RegistrationComponent } from "./registration.component";

@NgModule({
    declarations: [
        RegistrationComponent,
        RegistrationModalComponent,
    ],
    imports: [
        CommonModule,
        SharedModule,
        OeCheckboxComponent,
    ],
    exports: [
        RegistrationComponent,
    ],
})
export class RegistrationModule { }
