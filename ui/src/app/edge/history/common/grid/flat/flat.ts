import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

@Component({
  selector: 'gridWidget',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget { }
