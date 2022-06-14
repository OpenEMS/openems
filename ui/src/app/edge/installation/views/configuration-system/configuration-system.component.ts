import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentData } from 'src/app/shared/type/componentData';
import { environment } from 'src/environments';
import { Ibn } from '../../installation-systems/abstract-ibn';
import { HomeFeneconIbn } from '../../installation-systems/home-fenecon';
import { HomeHeckertIbn } from '../../installation-systems/home-heckert';

@Component({
  selector: ConfigurationSystemComponent.SELECTOR,
  templateUrl: './configuration-system.component.html'
})
export class ConfigurationSystemComponent implements OnInit {
  private static readonly SELECTOR = 'configuration-system';

  @Output() public nextViewEvent = new EventEmitter();
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;
  private ibn: Ibn;

  constructor() { }

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
        label = [{ value: 'heckert-home', label: 'Symphon-E' }];
        break;
      case 'FENECON':
      default:
        label = (
          [{ value: 'fenecon-home', label: 'Fenecon Home' }]);
        break;
    }

    fields.push({
      key: 'type',
      type: 'radio',
      templateOptions: {
        label: 'Speichertyp',
        type: 'radio',
        options: label,
        required: true
      }
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
    if (environment.theme === 'Heckert') {
      window.open('https://www.heckertsolar.com/wp-content/uploads/2022/03/Montage_und-Serviceanleitung-Symphon-E.pdf');
    } else {
      window.open('https://fenecon.de/wp-content/uploads/2022/02/V2022.01.27_DE_Montage-und_Serviceanleitung_Home.pdf');
    }
  }

  /**
   * Loads the appropriate Ibn object.
   */
  private setIbn() {
    //TODO Add the switch case to add appropriate IBN.
    if (this.form.controls['type'].value === 'heckert-home') {
      this.setIbnEvent.emit(new HomeHeckertIbn());
    } else {
      this.setIbnEvent.emit(new HomeFeneconIbn());
    }
    this.ibn = new HomeFeneconIbn();
  }
}
