import { Component } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { FieldWrapper } from "@ngx-formly/core";
import { ChannelAddress, Service, Websocket } from "../../shared";
import { DataService } from "../../genericComponents/shared/dataservice";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Subject } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";

@Component({
    selector: 'formly-field-modal',
    templateUrl: './formlyfieldmodal.html',
    providers: [{
        useClass: LiveDataService,
        provide: DataService
    }]
})
export class FormlyFieldModalComponent extends FieldWrapper {

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
            let formControls: Map<string, { channel: string, converter?: Function }> = new Map();

            this.props.lines
                .forEach(line => {
                    if (line.controlName) {
                        channels.push(ChannelAddress.fromString(line.channel));
                        formControls.set(line.controlName, { channel: line.channel, ...(line.converter && { converter: line.converter }) });
                    }
                });


            if (channels.length === 0) {
                return;
            }

            // Prefill formGroup
            formControls.forEach((channel, key) => {
                this.formGroup.registerControl(key, new FormControl(null));
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

                    let value;
                    if (control.converter != null && currentData) {
                        value = control.converter(currentData);
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