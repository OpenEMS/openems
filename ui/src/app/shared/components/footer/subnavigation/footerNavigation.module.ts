import { CommonModule } from "@angular/common";
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";

import { FormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { FlatWidgetButtonComponent } from "../../flat/flat-widget-button/flat-widget-button";
import { FooterNavigationComponent } from "./footerNavigation";
@NgModule({
    imports: [
        IonicModule,
        CommonModule,
        FormsModule,
        RouterModule,
        TranslateModule,
        FlatWidgetButtonComponent,
    ],
    declarations: [
        FooterNavigationComponent,
    ],
    exports: [
        FooterNavigationComponent,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class FooterNavigationComponentsModule { }

@NgModule({
    imports: [
        BrowserModule,
        FooterNavigationComponentsModule,
    ],
    declarations: [
    ],
    exports: [
        FooterNavigationComponentsModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class FooterNavigationModule { }
