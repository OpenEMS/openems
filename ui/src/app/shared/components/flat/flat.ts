// @ts-strict-ignore
import { Component, Input } from '@angular/core';
import { Icon, ImageIcon } from 'src/app/shared/type/widget';

@Component({
  selector: 'oe-flat-widget',
  templateUrl: './flat.html',
})
export class FlatWidgetComponent {

  /** Image in Header */
  @Input() public img?: ImageIcon;

  /** Icon in Header */
  @Input() public icon: Icon | null = null;

  /** BackgroundColor of the Header (light or dark) */
  @Input() public color?: string;

  /** Title in Header */
  @Input() public title?: string;
}
