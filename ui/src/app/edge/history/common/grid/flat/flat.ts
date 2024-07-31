import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';

@Component({
  selector: 'gridWidget',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget { }
