import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    CommonModule,
    IonicModule,
    TranslateModule,
  ],
  declarations: [
    FlatComponent,
    ModalComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class ControllerHeat { }
