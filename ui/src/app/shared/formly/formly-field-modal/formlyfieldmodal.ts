import { Component } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: 'formly-field-modal',
    templateUrl: './formlyfieldmodal.html'
})
export class FormlyFieldModalComponent extends FieldWrapper {

    protected formGroup: FormGroup = new FormGroup({});
    constructor(protected formBuilder: FormBuilder) {
        super();
    }

    ngOnInit() {
        console.log("props", this.props, this.options)

        // this.props.options.forEach(line => {
        //     if(line['controlName'] != null){

        //         if(line['channel']){

        //         }
        //         this.formGroup.registerControl(
        //             line['controlName'], new FormControl()
        //         )
        //     }
        // })
    }

    setFormGroup(event) {
        this.formGroup = event;
    }
}