import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

@Component({
    selector: 'simple',
    templateUrl: './simple.component.html',
})

export class SimpleComponent extends AbstractConfigForm {
    schedulerForm: FormGroup;

    constructor(
        websocketService: WebsocketService,
        private formBuilder: FormBuilder
    ) {
        super(websocketService);
    }

    @Input()
    set form(form: FormGroup) {
        this.schedulerForm = form;
    }

    /**
     * useless, need to be here because it's abstract in superclass
     */
    protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
        return;
    }

}