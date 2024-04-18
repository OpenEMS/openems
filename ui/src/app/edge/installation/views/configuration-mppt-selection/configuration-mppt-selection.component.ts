import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Home20FeneconIbn } from '../../installation-systems/home/home-20';
import { Home30FeneconIbn } from '../../installation-systems/home/home-30';

@Component({
  selector: ConfigurationMpptSelectionComponent.SELECTOR,
  templateUrl: './configuration-mppt-selection.component.html',
})
export class ConfigurationMpptSelectionComponent implements OnInit {
  private static readonly SELECTOR = 'configuration-mppt-selection';

  @Input() public ibn: Home20FeneconIbn | Home30FeneconIbn;
  @Output() public nextViewEvent = new EventEmitter();
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  protected form: FormGroup;
  protected fields: FormlyFieldConfig[];
  protected model;
  protected isNextDisabled: boolean;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {
    this.form = new FormGroup({});
    this.model = this.ibn.mppt;
    this.isNextDisabled = !this.model.connectionCheck;
    this.fields = this.getFields();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }
    this.ibn.mppt = this.model;
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {
    const fields: FormlyFieldConfig[] = [];

    for (let strings = 1; strings <= (this.ibn.maxNumberOfMppt !== -1 ? this.ibn.maxNumberOfMppt : this.ibn.maxNumberOfPvStrings); strings++) {

      const mppt = strings;
      const key: string = 'mppt' + mppt;
      const label: string = this.ibn.maxNumberOfMppt === -1 // Every second label has a different label.
        ? this.translate.instant('INSTALLATION.PROTOCOL_PV.MARKED_AS', { mppt: mppt, pv: strings })
        : this.translate.instant('INSTALLATION.PROTOCOL_PV.MARKED_AS_BOTH_STRINGS', { mppt: mppt, pv1: strings * 2 - 1, pv2: strings * 2 });
      const defaultValue: boolean = this.model[key];

      fields.push({
        key: key,
        props: {
          label: label,
          url: this.ibn.maxNumberOfMppt === -1 ? 'assets/img/home-mppt/' + mppt + '.' + strings + '.png' : this.ibn.getImageUrl(mppt),
        },
        defaultValue: defaultValue,
        wrappers: ['formly-field-checkbox-with-image'],
      });
    }

    fields.push({
      key: 'connectionCheck',
      type: 'checkbox',
      props: {
        label: 'Wurden alle Anschlüsse kontrolliert?',
        required: true,
        change: (field) => {
          this.isNextDisabled = !field.formControl.value;
        },
      },
      defaultValue: false,
    });

    return fields;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

}
