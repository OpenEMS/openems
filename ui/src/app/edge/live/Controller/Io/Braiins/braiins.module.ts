import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerBraiinsHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        SharedModule,
        ModalModule,
        CommonModule,
        ComponentsBaseModule,
        ControllerBraiinsHomeComponent,
    ],
    declarations: [],
    exports: [ControllerBraiinsHomeComponent],
})
export class ControllerIoFixDigitalOutput {}
