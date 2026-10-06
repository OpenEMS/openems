import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { RangeSliderComponent } from "src/app/shared/components/range-slider/range-slider";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { PopoverComponent } from "./popover/popover";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        RangeSliderComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        PopoverComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Evcs { }
