import { NgModule } from "@angular/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { CommonConsumptionHistory } from "../../../common/consumption/history/consumption-history";
import { ControllerIoHeatpumpComponent } from "./flat/flat";
import { ControllerIoHeatpumpModalComponent } from "./modal/modal";
import { ControllerIoHeatpumpHomeComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatpumpSettingsComponent } from "./settings/settings";

@NgModule({
    imports: [
        SharedModule,
        ModalModule,
        CommonConsumptionHistory,
        ComponentsBaseModule,
        ControllerIoHeatpumpSettingsComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
        ControllerIoHeatpumpHomeComponent,
    ],
    exports: [
        ControllerIoHeatpumpHomeComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
    ],
})
export class ControllerIoHeatpumpModule { }
