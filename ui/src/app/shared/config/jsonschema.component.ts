import { Component, Input, Output, OnChanges, OnDestroy, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { DefaultMessages } from '../service/defaultmessages';
import { Utils } from '../service/utils';
import { ConfigImpl } from '../device/config';
import { Device } from '../device/device';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';

interface JsonSchema {
  type: 'array' | 'object' | 'string',
  items?: JsonSchema,
  properties?: {
    [thing: string]: JsonSchema
  }
}

@Component({
  selector: 'jsonschema',
  templateUrl: './jsonschema.component.html'
})
export class JsonSchemaComponent implements OnChanges {

  constructor(private formBuilder: FormBuilder) { }

  private _schema: JsonSchema = null;
  private _value: any = null;
  private form: FormGroup = null;
  private stopOnChange: Subject<void> = new Subject<void>();

  @Output() onChange = new EventEmitter<Object>();

  @Input() set value(value: string) {
    this._value = JSON.parse(value);
  };

  @Input() set schema(schema: string) {
    this._schema = JSON.parse(schema);
  };

  ngOnChanges() {
    if (this._value == null || this._schema == null) {
      return;
    }

    let form = this.buildForm(this._value, this._schema);

    // create wrapper group because angular always needs a group as basis
    this.form = this.formBuilder.group({ form: form });
    this.stopOnChange.next();
    this.form.valueChanges.takeUntil(this.stopOnChange).subscribe(value => {
      this.onChange.emit(value.form);
    })
  }

  protected buildForm(value: any, schema: JsonSchema, property?: string): FormControl | FormGroup | FormArray {
    switch (schema.type) {
      case 'array':
        return this.buildFormArray(value, schema, property);
      case 'object':
        return this.buildFormGroup(value, schema, property);
      case 'string':
        return this.buildFormControl(value, schema);
    }
  }

  private buildFormArray(value: any, schema: JsonSchema, property: string): FormArray {
    var builder: any[] = [];
    for (let entry of value as any[]) {
      let form = this.buildForm(entry, schema.items, property);
      if (form) {
        form["_meta"] = schema.items;
        builder.push(form);
      }
    }
    return this.formBuilder.array(builder);
  }

  private buildFormGroup(value: any, schema: JsonSchema, property: string): FormGroup {
    let group: { [key: string]: any } = {};
    for (let property in schema.properties) {
      let form = this.buildForm(value[property], schema.properties[property], property);
      if (form != null) {
        // form["_meta"] = schema;
        group[property] = form;
      }
    }
    return this.formBuilder.group(group);
  }

  private buildFormControl(value: any, schema: JsonSchema): FormControl {
    let form = this.formBuilder.control(value);
    form["_meta"] = schema;
    return form;
  }

  private addToTimeControllers(form: FormArray) {
    form.push(this.formBuilder.group({
      time: this.formBuilder.control(""),
      controllers: this.formBuilder.array([
        this.formBuilder.control("")
      ])
    }));
  }

  private addToControllers(controllers: FormArray) {
    controllers.push(this.formBuilder.control(""));
  }

  private removeFromControllers(controllers: FormArray, index: number) {
    controllers.removeAt(index);
  }

  private removeFromTimeControllers(form: FormArray, index: number) {
    form.removeAt(index);
  }

  private trackByFn(index: any, item: any) {
    return index;
  }
}
