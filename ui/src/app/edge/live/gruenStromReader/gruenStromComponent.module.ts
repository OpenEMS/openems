import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { GruenStromComponent } from "./greunStromComponent_flat";
import { GruenStromModalComponent } from "./modal/gruenStrom_modal";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        GruenStromComponent,
        GruenStromModalComponent,
    ],
    exports: [
        GruenStromComponent,
    ],
})
export class Reader_Green_Power { }
