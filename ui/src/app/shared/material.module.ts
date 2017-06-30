import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { MaterialModule } from '@angular/material';

//TODO list only components that are used (reference: https://github.com/angular/material2/releases/tag/2.0.0-beta.3)

@NgModule({
  imports: [
    MaterialModule
  ],
  declarations: [
  ],
  exports: [
    MaterialModule
  ]
})
export class MyMaterialModule { }
