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
  protected showManual = false;

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
    let options: ComponentData[] = [];

    switch (environment.theme) {
      case 'Heckert':
        options = [{ value: SystemType.HECKERT_HOME, label: SystemType.HECKERT_HOME }];
        this.showManual = true;
        break;
      case 'FENECON':
      default:
        options = [
          { value: SystemType.FENECON_HOME, label: SystemType.FENECON_HOME },
          { value: SystemType.COMMERCIAL, label: SystemType.COMMERCIAL }];
        break;
    }

    fields.push({
      key: 'type',
      type: 'radio',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SYSTEM.PRODUCT_NAME'),
        type: 'radio',
        options: options,
        required: true,
      },
    });
    return fields;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  /**
   * Loads the appropriate Ibn object.
   */
  private setIbn(): void {
    const system: SystemType = this.form.controls.type.value;
    this.ibn = System.getSystemObjectFromSystemType(system, this.translate);
  }

  /**
   * Redirects to the appropriate url for Heckert system manual.
   */
  protected openManual(): void {
    window.open(environment.links.MANUALS.HOME.HOME_10);
  }
}
