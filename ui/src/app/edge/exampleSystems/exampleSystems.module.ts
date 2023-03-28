import { NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { Generic_ComponentsModule } from "src/app/shared/genericComponents/genericComponents";
import { FormlyFieldModalComponent } from "./formly/formlyfieldmodal";

@NgModule({
  imports: [
    FormlyModule.forRoot({
      wrappers: [
        { name: 'formly-field-modal', component: FormlyFieldModalComponent }]
    }),
    BrowserModule,
    IonicModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule.forRoot(),
    Generic_ComponentsModule
  ],
  declarations: [
    FormlyFieldModalComponent
  ],
  exports: [FormlyModule,
  ]
})

export class ExampleSystemsModule { }