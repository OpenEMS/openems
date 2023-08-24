import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { FieldWrapper } from "@ngx-formly/core";
import { Subject } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";
import { LiveDataService } from "src/app/edge/live/livedataservice";

import { DataService } from "../../genericComponents/shared/dataservice";
import { ChannelAddress, Service, Websocket } from "../../shared";

@Component({
    selector: 'formly-field-modal',
    templateUrl: './formlyfieldmodal.html',
    providers: [{
        useClass: LiveDataService,
        provide: DataService
    }]
})
export class FormlyFieldModalComponent extends FieldWrapper implements OnInit, OnDestroy {

    protected formGroup: FormGroup = new FormGroup({});
    private stopOnDestroy: Subject<void> = new Subject<void>();
    constructor(
        protected formBuilder: FormBuilder,
        protected service: Service,
        protected websocket: Websocket,
        protected dataService: DataService
    ) {
        super();
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            let channels: ChannelAddress[] = [];
            let formControls: Map<string, { channel: string, converter?: Function, valueChanges?: (formGroup: FormGroup, controlValue: number | string | null) => FormGroup }> = new Map();

            this.props.lines
                .forEach(line => {
                    if (line.controlName) {
                        channels.push(ChannelAddress.fromString(line.channel));
                        formControls.set(line.controlName, { channel: line.channel, ...(line.converter && { converter: line.converter }), ...(line.valueChanges && { valueChanges: line.valueChanges }) });
                    }
                });


            if (channels.length === 0) {
                return;
            }

            // Prefill formGroup
            formControls.forEach((config, key) => {
                this.formGroup.registerControl(key, new FormControl(null));

                if (config.valueChanges) {
                    this.formGroup.controls[key].valueChanges.subscribe((value) => {
                        this.formGroup = config.valueChanges(this.formGroup, value);
                    });
                }
            });

            this.dataService.getValues(channels, edge, this.props.component.id);
            this.dataService.currentValue.pipe(takeUntil(this.stopOnDestroy), filter(currentData => !!currentData)).subscribe(currentData => {
                formControls.forEach((control, key) => {

                    // If value for channel equals null or undefined, skip wrong value
                    if (currentData.allComponents[control.channel] === null || currentData.allComponents[control.channel] === undefined) {
                        return;
                    }

                    // If formGroup is dirty, stop overwriting
                    if (this.formGroup.controls[key]?.dirty) {
                        return;
                    }

                    let value: number | string | null;
                    if (control.converter != null) {
                        let channel = currentData.allComponents[control.channel];
                        value = control.converter(channel);
                    } else {
                        value = currentData.allComponents[control.channel];
                    }

                    this.formGroup.controls[key].setValue(value);
                    this.formGroup.controls[key].markAsPristine();
                });
            });
        });
    }

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}