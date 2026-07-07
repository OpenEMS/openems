import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateModule } from "@ngx-translate/core";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { CommonSelfConsumptionHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        TranslateModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        CommonSelfConsumptionHomeComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class CommonSelfconsumption { }
