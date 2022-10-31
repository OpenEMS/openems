import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Commercial30AnschlussIbn } from '../../installation-systems/commercial/commercial-30/commercial30-anschluss';
import { Commercial50EigenverbrauchsOptimierung } from '../../installation-systems/commercial/commercial-50/commercial50-eigenverbrauchsoptimierung';
import { HomeFeneconIbn } from '../../installation-systems/home/home-fenecon';
import { HomeHeckertIbn } from '../../installation-systems/home/home-heckert';
import { ComponentData } from '../../shared/ibndatatypes';

@Component({
  selector: ConfigurationSystemComponent.SELECTOR,
  templateUrl: './configuration-system.component.html'
})
export class ConfigurationSystemComponent implements OnInit {
  private static readonly SELECTOR = 'configuration-system';

  @Input() public ibn: AbstractIbn;
  @Output() public nextViewEvent = new EventEmitter();
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  protected form: FormGroup;
  protected fields: FormlyFieldConfig[];
  protected model;

  constructor(private translate: TranslateService,) { }

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
          [{ value: 'home', label: 'FENECON Home' },
          { value: 'commercial-30', label: 'FENECON Commercial 30' },
          { value: 'commercial-50', label: 'FENECON Commercial 50' }
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

    const system = this.form.controls['type'].value;

    switch (system) {
      case 'heckert-home':
        window.open('https://www.heckertsolar.com/wp-content/uploads/2022/03/Montage_und-Serviceanleitung-Symphon-E.pdf');
        break;
      case 'home':
        window.open('https://fenecon.de/download/montage-und-serviceanleitung-feneconhome/?wpdmdl=17765&refresh=62a048d9acf401654671577');
        break;
      case 'commercial-30':
        window.open('https://fenecon.de/downloadcenter-commercial-30/');
        break;
      case 'commercial-50':
        window.open('https://fenecon.de/downloadcenter-commercial-50/');
        break;
    }
  }

  /**
   * Loads the appropriate Ibn object.
   */
  private setIbn() {
    const system = this.form.controls['type'].value;

    switch (system) {
      case 'heckert-home':
        this.ibn = new HomeHeckertIbn(this.translate);
        break;
      case 'home':
        this.ibn = new HomeFeneconIbn(this.translate);
        break;
      case 'commercial-30':
        this.ibn = new Commercial30AnschlussIbn(this.translate);
        break;
      case 'commercial-50':
        this.ibn = new Commercial50EigenverbrauchsOptimierung(this.translate);
        break;
    }
  }
}
