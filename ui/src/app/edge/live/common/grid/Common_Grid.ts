import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { CommonGridHistory } from "./history/grid-history";
import { ModalComponent } from "./modal/modal";
import { CommonGridHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        CommonGridHistory,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        CommonGridHomeComponent,
    ],
    exports: [
        FlatComponent,
        CommonGridHomeComponent,
    ],
})
export class Common_Grid { }
