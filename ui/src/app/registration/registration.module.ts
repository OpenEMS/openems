import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { SharedModule } from "../shared/shared.module";
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
  ],
  exports: [
    RegistrationComponent,
  ],
})
export class RegistrationModule { }
