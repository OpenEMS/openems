import { CommonModule } from "@angular/common";
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";

import { TranslateModule } from "@ngx-translate/core";
import { PipeComponentsModule } from "../../pipe/pipe.module";
import { OeImageComponent } from "../oe-img/oe-img";
import { HelpButtonComponent } from "./help-button/help-button";
import { HelpLinkComponent } from "./help-link/help-link";
import { ModalComponent } from "./modal";
import { ModalButtonsComponent } from "./modal-button/modal-button";
import { ModalInfoLineComponent } from "./modal-info-line/modal-info-line";
import { ModalLineComponent } from "./modal-line/modal-line";
import { ModalLineItemComponent } from "./modal-line/modal-line-item/modal-line-item";
import { ModalPhasesComponent } from "./modal-phases/modal-phases";
import { ModalValueLineComponent } from "./modal-value-line/modal-value-line";
import { ModalHorizontalLineComponent } from "./model-horizontal-line/modal-horizontal-line";

@NgModule({
  imports: [
    ReactiveFormsModule,
    FormsModule,
    IonicModule,
    CommonModule,
    PipeComponentsModule,
    TranslateModule,
    HelpButtonComponent,
    OeImageComponent,
  ],
  declarations: [
    ModalButtonsComponent,
    ModalInfoLineComponent,
    ModalLineComponent,
    ModalHorizontalLineComponent,
    ModalComponent,
    ModalLineItemComponent,
    ModalPhasesComponent,
    ModalValueLineComponent,
    HelpLinkComponent,
  ],
  exports: [
    ModalButtonsComponent,
    ModalInfoLineComponent,
    ModalLineComponent,
    ModalHorizontalLineComponent,
    ModalComponent,
    ModalLineItemComponent,
    ModalPhasesComponent,
    ModalValueLineComponent,
    OeImageComponent,
    HelpLinkComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ModalComponentsModule { }
@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    ReactiveFormsModule,
    RouterModule,
    FormsModule,
    TranslateModule,
    PipeComponentsModule,
    ModalComponentsModule,
  ],
  exports: [
    ModalComponentsModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})

export class ModalModule { }

