import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        FlatComponent,
        ModalComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Ess_TimeOfUseTariff_Discharge { }
