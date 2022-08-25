import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormlyFieldConfig } from '@ngx-formly/core';

@Component({
  selector: InstallationViewComponent.SELECTOR,
  templateUrl: './installation-view.component.html'
})
export class InstallationViewComponent {

  private static readonly SELECTOR = "installation-view";

  @Input() public iconName: string;
  @Input() public header: string;
  @Input() public subtitle: string;
  @Input() public fields: FormlyFieldConfig[];

  @Input() public isWaiting: boolean = false;
  @Input() public isFirstView: boolean = false;
  @Input() public isLastView: boolean = false;

  @Output() public previousClicked: EventEmitter<any> = new EventEmitter();
  @Output() public nextClicked: EventEmitter<any> = new EventEmitter();

  constructor() { }

  public keyPressed(event) {
    if (event.key === 'Enter') this.nextClicked.emit();
  }
}