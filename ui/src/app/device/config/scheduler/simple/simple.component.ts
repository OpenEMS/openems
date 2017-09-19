import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { Websocket } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

@Component({
  selector: 'simple',
  templateUrl: './simple.component.html',
})

export class SimpleComponent extends AbstractConfigForm {
  schedulerForm: FormGroup;

  constructor(
    websocket: Websocket,
    private formBuilder: FormBuilder
  ) {
    super(websocket);
  }

  @Input()
  set form(form: AbstractControl) {
    if (form instanceof FormGroup) {
      this.schedulerForm = form;
    }
  }

  /**
   * useless, need to be here because it's abstract in superclass
   */
  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }

}