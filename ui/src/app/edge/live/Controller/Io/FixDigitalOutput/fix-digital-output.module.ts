import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerFixDigitalOutputComponent } from "./flat/flat";
import { FixDigitalOutputHistory } from "./history/fix-digital-output-history.module";
import { ControllerFixDigitalOutputModalComponent } from "./modal/modal";

@NgModule({
    imports: [
        SharedModule,
        ModalModule,
        CommonModule,
        ComponentsBaseModule,
        FixDigitalOutputHistory,
    ],
    declarations: [
        ControllerFixDigitalOutputComponent,
        ControllerFixDigitalOutputModalComponent,
    ],
    exports: [
        ControllerFixDigitalOutputComponent,
        ControllerFixDigitalOutputModalComponent,
        FixDigitalOutputHistory,
    ],
})
export class ControllerIoFixDigitalOutput { }
