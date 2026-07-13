import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerModbusTcpApiDetailsComponent } from "./details/details";
import { FlatComponent } from "./flat/flat";
import { ControllerModbusTcpApiHistoryComponent } from "./history/new-navigation/new-navigation";
import { ModalComponent } from "./modal/modal";
import { ControllerModbusTcpApiHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        ControllerModbusTcpApiHistoryComponent,
        ControllerModbusTcpApiDetailsComponent,
        ControllerModbusTcpApiHomeComponent,
    ],
    declarations: [FlatComponent, ModalComponent],
    exports: [
        FlatComponent,
        ControllerModbusTcpApiHomeComponent,
        ControllerModbusTcpApiDetailsComponent,
        ControllerModbusTcpApiHistoryComponent,
    ],
})
export class Controller_Api_ModbusTcp {}
