import { CommonModule } from "@angular/common";
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";

import { DirectiveModule } from "../../../directive/directive";
import { FooterNavigationComponent } from "./footerNavigation";
@NgModule({
  imports: [
    IonicModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
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
    IonicModule,
    CommonModule,
    DirectiveModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
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
