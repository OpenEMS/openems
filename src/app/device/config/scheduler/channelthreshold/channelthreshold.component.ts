import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../../../service/websocket.service';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

@Component({
    selector: 'form-scheduler-channelthreshold',
    templateUrl: './channelthreshold.component.html',
})

export class FormSchedulerChannelthresholdComponent extends AbstractConfigForm {
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

    protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
        return;
    }

    addControllerToThreshold(thresholdForm: FormArray) {
        // console.log("addControllerToThreshold");
        thresholdForm.push(this.formBuilder.control(""));
        // console.log(this.schedulerForm.controls['scheduler']['controls']['thresholds']);
        // console.log(thresholdForm);
        thresholdForm.markAsDirty();
    }

    removeControllerFromThreshold(thresholdForm: FormArray, index: number) {
        // console.log(thresholdForm);
        thresholdForm.removeAt(index);
        thresholdForm.markAsDirty();
    }

    addThreshold(thresholdForm: FormArray) {
        // console.log("addThreshold");
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