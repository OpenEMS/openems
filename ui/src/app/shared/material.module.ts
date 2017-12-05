import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import {
  MatCardModule,
  MatSlideToggleModule,
  MatSidenavModule,
  MatListModule,
  MatIconModule,
  MatButtonModule,
  MatToolbarModule,
  MatInputModule
} from '@angular/material';

//TODO list only components that are used (reference: https://github.com/angular/material2/releases/tag/2.0.0-beta.3)

@NgModule({
  imports: [
    MatCardModule,
    MatSlideToggleModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule,
    MatInputModule
  ],
  declarations: [
  ],
  exports: [
    MatCardModule,
    MatSlideToggleModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule,
    MatInputModule
  ]
})
export class MyMaterialModule { }
