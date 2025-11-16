import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { CommonConsumptionDetailsComponent } from "./details/details";
import { CommonConsumptionGeneralComponent } from "./flat/flat";
import { CommonConsumptionHistory } from "./history/consumption-history";
import { ModalComponent } from "./modal/modal";
import { CommonConsumptionHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        CommonConsumptionHistory,
        ComponentsBaseModule,
    ],
    declarations: [
        CommonConsumptionGeneralComponent,
        ModalComponent,
        CommonConsumptionHomeComponent,
        CommonConsumptionDetailsComponent,
    ],
    exports: [
        CommonConsumptionGeneralComponent,
        CommonConsumptionHomeComponent,
        CommonConsumptionDetailsComponent,
    ],
})
export class CommonConsumption { }
