import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { OeCheckboxComponent } from "src/app/shared/components/oe-checkbox/oe-checkbox";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { RegistrationModalComponent } from "./modal/MODAL.COMPONENT";
import { RegistrationComponent } from "./REGISTRATION.COMPONENT";

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
