import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { ComponentData } from '../../shared/ibndatatypes';
import { System, SystemType } from '../../shared/system';

@Component({
  selector: ConfigurationSystemComponent.SELECTOR,
  templateUrl: './configuration-system.component.html',
})
export class ConfigurationSystemComponent implements OnInit {
  private static readonly SELECTOR = 'configuration-system';

  @Input() public ibn: AbstractIbn;
  @Output() public nextViewEvent = new EventEmitter();
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  protected form: FormGroup;
  protected fields: FormlyFieldConfig[];
  protected model;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {
    this.form = new FormGroup({});
    this.fields = this.getFields();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }
    // Sets the ibn object.
    this.setIbn();
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {
    const fields: FormlyFieldConfig[] = [];
    let label: ComponentData[] = [];

    switch (environment.theme) {
      case 'Heckert':
        label = [{ value: SystemType.HECKERT_HOME_10, label: System.getSystemTypeLabel(SystemType.HECKERT_HOME_10) }];
        break;
      case 'FENECON':
      default:
        label = (
          [{ value: SystemType.FENECON_HOME_10, label: System.getSystemTypeLabel(SystemType.FENECON_HOME_10) },
          { value: SystemType.FENECON_HOME_20, label: System.getSystemTypeLabel(SystemType.FENECON_HOME_20) },
          { value: SystemType.FENECON_HOME_30, label: System.getSystemTypeLabel(SystemType.FENECON_HOME_30) },
          { value: SystemType.COMMERCIAL_30, label: System.getSystemTypeLabel(SystemType.COMMERCIAL_30) },
          { value: SystemType.COMMERCIAL_50, label: System.getSystemTypeLabel(SystemType.COMMERCIAL_50) },
          ]);
        break;
    }

    fields.push({
      key: 'type',
      type: 'radio',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SYSTEM.PRODUCT_NAME'),
        type: 'radio',
        options: label,
        required: true,
      },
    });
    return fields;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  /**
   * Redirects to the appropriate url for system manual.
   */
  public openManual() {
    const system = this.form.controls.type.value;
    window.open(System.getSystemTypeLink(system));
  }

  /**
   * Loads the appropriate Ibn object.
   */
  private setIbn() {
    const system = this.form.controls.type.value;
    this.ibn = System.getSystemObjectFromSystemType(system, this.translate);
  }
}
