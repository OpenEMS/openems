import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: InstallationViewComponent.SELECTOR,
  templateUrl: './installation-view.component.html',
})
export class InstallationViewComponent implements OnChanges {

  private static readonly SELECTOR = "installation-view";

  @Input() public iconName: string;
  @Input() public header: string;
  @Input() public subtitle: string;
  @Input() public fields: FormlyFieldConfig[];

  @Input() public isWaiting: boolean = false;
  @Input() public isNextDisabled: boolean = false;
  @Input() public isFirstView: boolean = false;
  @Input() public isLastView: boolean = false;

  @Output() public previousClicked: EventEmitter<any> = new EventEmitter();
  @Output() public nextClicked: EventEmitter<any> = new EventEmitter();
  public labelToShow: { label: string, icon: string } | null = null;

  constructor(private translate: TranslateService) { }

  // often 'isWaiting' changes its value after initialization and ngOnInit dose not record the changed values,
  // So using the ngOnChanges.
  public ngOnChanges() {
    if (this.isWaiting) {
      this.setLabelAndIcon(this.translate.instant('INSTALLATION.LOAD'), 'hourglass-outline');
    } else if (this.isLastView) {
      this.setLabelAndIcon(this.translate.instant('INSTALLATION.COMPLETE'), 'checkmark-done-outline1');
    } else {
      this.setLabelAndIcon(this.translate.instant('INSTALLATION.NEXT'), 'arrow-forward-outline');
    }
  }

  public keyPressed(event) {
    if (event.key === 'Enter') {
      this.nextClicked.emit();
    }
  }

  /**
   * Sets the label and icons for the button.
   *
   * @param labelKey Translated labels.
   * @param icon Icon to be displayed
   */
  private setLabelAndIcon(labelKey: string, icon: string) {
    this.labelToShow = {
      label: labelKey,
      icon: icon,
    };
  }
}
