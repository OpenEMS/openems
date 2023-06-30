import { Component, Input } from '@angular/core';
import { Icon } from 'src/app/shared/type/widget';

@Component({
  selector: 'oe-flat-widget',
  templateUrl: './flat.html'
})
export class FlatWidgetComponent {

  /** Image in Header */
  @Input() public img: string;

  /** Icon in Header */
  @Input() public icon: Icon = null;

  /** BackgroundColor of the Header (light or dark) */
  @Input() public color: string;

  /** Title in Header */
  @Input() public title: string;
}