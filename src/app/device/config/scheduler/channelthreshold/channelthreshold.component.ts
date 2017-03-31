import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

@Component({
    selector: 'channelthreshold',
    templateUrl: './channelthreshold.component.html',
})
export class ChannelthresholdComponent extends AbstractConfigForm {

    public schedulerForm: FormGroup;

    constructor(
        public websocketService: WebsocketService,
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

    addControllerToThreshold(thresholdForm: FormArray) {
        thresholdForm.push(this.formBuilder.control(""));
        thresholdForm.markAsDirty();
    }

    removeControllerFromThreshold(thresholdForm: FormArray, index: number) {
        thresholdForm.removeAt(index);
        thresholdForm.markAsDirty();
    }

    addThreshold(thresholdForm: FormArray) {
        thresholdForm.push(this.formBuilder.group({
            "threshold": this.formBuilder.control(""),
            "hysteresis": this.formBuilder.control(""),
            "controller": this.formBuilder.array([])
        }))
    }

    removeThreshold(thresholdForm: FormArray, index: number) {
        thresholdForm.removeAt(index);
        thresholdForm.markAsDirty();
    }

}