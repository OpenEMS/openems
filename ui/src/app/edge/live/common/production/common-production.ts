import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { CommonProductionDetailsComponent } from "./details/details";
import { FlatComponent } from "./flat/flat";
import { CommonProductionHistory } from "./history/production-history";
import { ModalComponent } from "./modal/modal";
import { CommonProductionHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        CommonProductionHistory,
        ComponentsBaseModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        CommonProductionHomeComponent,
        CommonProductionDetailsComponent,
    ],
    exports: [
        CommonProductionHomeComponent,
        FlatComponent,
    ],
})
export class CommonProduction { }
