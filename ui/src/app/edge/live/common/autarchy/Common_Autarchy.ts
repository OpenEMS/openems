import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { CommonAutarchyHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        CommonAutarchyHomeComponent,
    ],
    exports: [
        FlatComponent,
        CommonAutarchyHomeComponent,
    ],
})
export class Common_Autarchy { }
