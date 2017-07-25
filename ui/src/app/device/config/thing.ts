import { FormGroup, FormBuilder, FormControl, FormArray, AbstractControl } from '@angular/forms';
import { Device } from '../../shared/shared';

type ConfigureRequestModeType = "update" | "create" | "delete";
class ConfigureRequest {
    mode: ConfigureRequestModeType;
}
interface ConfigureUpdateRequest extends ConfigureRequest {
    thing: string
    channel: string;
    value: Object;
}
interface ConfigureCreateRequest extends ConfigureRequest {
    object: Object;
    parent: string;
}
interface ConfigureDeleteRequest extends ConfigureRequest {
    thing: string;
}
interface ConfigureUpdateSchedulerRequest extends ConfigureRequest {
    thing: string
    class: string;
    value: Object;
}

export class Thing {

    public form: FormGroup;

    constructor(private formBuilder: FormBuilder) {
    }

    protected buildForm(item: any, ignoreKeys?: string | string[]): FormControl | FormGroup | FormArray {
        if (typeof item === "function") {
            // ignore
        } else if (item instanceof Array) {
            return this.buildFormArray(item, ignoreKeys);
        } else if (item instanceof Object) {
            return this.buildFormGroup(item, ignoreKeys);
        } else {
            return this.buildFormControl(item, ignoreKeys);
        }
    }

    protected buildFormGroup(object: any, ignoreKeys?: string | string[]): FormGroup {
        let group: { [key: string]: any } = {};
        for (let key in object) {
            if ((typeof ignoreKeys === "string" && key == ignoreKeys) || (typeof ignoreKeys === "object") && ignoreKeys.some(ignoreKey => ignoreKey === key)) {
                // ignore
            } else {
                var form = this.buildForm(object[key], ignoreKeys);
                if (form) {
                    group[key] = form;
                }
            }
        }
        return this.formBuilder.group(group);
    }

    private buildFormControl(item: Object, ignoreKeys?: string | string[]): FormControl {
        return this.formBuilder.control(item);
    }

    private buildFormArray(array: any[], ignoreKeys?: string | string[]): FormArray {
        var builder: any[] = [];
        for (let item of array) {
            var control = this.buildForm(item, ignoreKeys);
            if (control) {
                builder.push(control);
            }
        }
        return this.formBuilder.array(builder);
    }

    /**
     * Delete value from channel array
     */
    deleteFromChannel(channelName: string, indexChannel: number): void {
        let channelArray: FormArray = <FormArray>this.form.controls[channelName];
        channelArray.removeAt(indexChannel);
        channelArray.markAsDirty();
    }

    /**
     * Delete value to channel array
     */
    addToChannel(channelName: string): void {
        let channelArray: FormArray = <FormArray>this.form.controls[channelName];
        channelArray.push(this.formBuilder.control(""));
        channelArray.markAsDirty();
    }

    /**
     * Send configure request(s) to websocket
     */
    protected send(requests: ConfigureRequest[], device: Device) {
        if (requests.length > 0) {
            if (device != null) {
                device.send({
                    configure: requests
                });
            } else {
                // TODO: error message: no current device!
            }
        }
    }

    /**
     * Create a ConfigUpdateRequests to save changes to the form
     */
    protected getConfigUpdateRequests(form: AbstractControl): ConfigureRequest[] {
        let requests: ConfigureRequest[] = [];
        if (form instanceof FormGroup) {
            let formControl = form.controls;
            let id = formControl['id'].value;
            for (let key in formControl) {
                if (formControl[key].dirty) {
                    let value = formControl[key].value;
                    requests.push(<ConfigureUpdateRequest>{
                        mode: "update",
                        thing: id,
                        channel: key,
                        value: value
                    });
                }
            }
        }
        return requests;
    }
}