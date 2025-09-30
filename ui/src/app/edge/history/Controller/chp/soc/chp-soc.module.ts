import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { FlatComponent } from "./flat/flat";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class ChpSoc { }
