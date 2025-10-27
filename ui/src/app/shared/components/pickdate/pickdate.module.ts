import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AngularMyDatePickerModule } from "@nodro7/angular-mydatepicker";
import { PickDateComponent } from "./pickdate.component";
import { PickDatePopoverComponent } from "./popover/popover.component";

@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    TranslateModule,
    AngularMyDatePickerModule,
    ReactiveFormsModule,
  ],
  declarations: [
    PickDateComponent,
    PickDatePopoverComponent,
  ],
  exports: [
    PickDateComponent,
  ],
})
export class PickdateComponentModule { }
@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    TranslateModule,
    PickdateComponentModule,
  ],
  exports: [
    PickdateComponentModule,
  ],
})
export class PickdateModule { }
