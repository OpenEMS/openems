import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    ModalComponentsModule,
  ],
  declarations: [
    FlatComponent,
    ModalComponent,
  ],
  exports: [
    FlatComponent,
    ModalComponentsModule,
  ],
})
export class Common_Consumption { }
