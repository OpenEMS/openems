import { Component, Input, Output, OnChanges, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { DefaultMessages } from '../service/defaultmessages';
import { Utils } from '../service/utils';
import { ConfigImpl } from '../device/config';
import { Device } from '../device/device';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';

@Component({
  selector: 'channel',
  templateUrl: './channel.component.html'
})
export class ChannelComponent implements OnChanges, OnDestroy {

  public form: FormGroup = null;
  public meta = null;
  public type: string | string[];
  public specialType: "simple" | "ignore" | "boolean" | "selectNature" | "thing" = "simple";
  public deviceNature: string = null;
  public allowWrite = false;
  public allowRead = false;

  private isJson = false;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  @Input() public thingId: string = null;
  @Input() public channelId: string = null;
  @Input() public config: ConfigImpl = null;
  @Input() public role: Role = "guest"; // TODO in device
  @Input() public device: Device = null;
  @Input() public showThings: boolean = false;
  @Input() public title: string = null;

  @Output() public message: Subject<DefaultTypes.ConfigUpdate> = new Subject<DefaultTypes.ConfigUpdate>();

  constructor(
    public utils: Utils,
    private formBuilder: FormBuilder) { }

  ngOnChanges() {
    if (this.config == null || this.thingId == null || !(this.thingId in this.config.things) || this.channelId == null) {
      return;
    }

    let thingConfig = this.config.things[this.thingId];
    let clazz = thingConfig.class;
    if (clazz instanceof Array) {
      return;
    }
    let thingMeta = this.config.meta[clazz];
    let channelMeta = thingMeta.channels[this.channelId];
    this.meta = channelMeta;
    if (this.title == null) {
      this.title = channelMeta.title;
    }

    // handle access role
    this.allowWrite = this.meta.writeRoles.includes(this.role);
    this.allowRead = this.meta.readRoles.includes(this.role);
    if (!this.allowRead) {
      return;
    }

    // get value or default value
    let value = thingConfig[this.channelId];
    if (value == null) {
      value = channelMeta.defaultValue;
    }
    if (this.meta.array == true && value === "") {
      // value is still not available and we have an array type: initialize array
      value = [];
    }

    // set form input type and specialType-flag
    let metaType = this.meta.type;
    switch (metaType) {
      case 'Boolean':
        this.specialType = 'boolean';
        break;

      case 'Integer':
      case 'Long':
        this.type = 'number';
        break;

      case 'String':
      case 'Inet4Address':
        this.type = 'string';
        break;

      case 'JsonArray':
      case 'JsonObject':
        this.specialType = 'simple';
        this.isJson = true;
        value = JSON.stringify(value);
        break;

      default:
        if (metaType in this.config.meta) {
          // this is a DeviceNature - will be handled as separate thing -> ignore
          if (this.showThings) {
            this.specialType = 'thing';
          } else {
            this.specialType = 'ignore';
          }
        } else if (this.meta.type instanceof Array && this.meta.type.includes("DeviceNature")) {
          // this channel takes references to a DeviceNature (like "ess0" for SymmetricEssNature)
          this.specialType = 'selectNature';
          this.type = "string";
          // takes the first nature as requirement;
          // e.g. takes "AsymmetricEssNature" from ["AsymmetricEssNature", "EssNature", "DeviceNature"]
          this.deviceNature = this.meta.type[0];
        } else {
          console.warn("Unknown type: " + this.meta.type, this.meta);
          this.type = 'string';
        }
    }

    // build form
    this.form = this.buildFormGroup({ channelConfig: value });

    // subscribe to form changes and build websocket message
    this.form.valueChanges
      .takeUntil(this.stopOnDestroy)
      .subscribe(() => this.updateMessage());
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  private updateMessage() {
    if (!this.allowWrite) return;
    let value = this.form.value["channelConfig"];
    if (this.isJson) {
      try {
        value = JSON.parse(value);
        this.message.next(DefaultMessages.configUpdate(this.thingId, this.channelId, value));
      } catch (e) {
        this.message.next(null);
      }
    } else {
      this.message.next(DefaultMessages.configUpdate(this.thingId, this.channelId, value));
    }

  }

  public addToArray() {
    if (!this.allowWrite) return;
    let array = <FormArray>this.form.controls["channelConfig"];
    array.push(this.formBuilder.control(""));
    this.form.markAsDirty();
    this.updateMessage();
  }

  public removeFromArray(index: number) {
    if (!this.allowWrite) return;
    let array = <FormArray>this.form.controls["channelConfig"];
    array.removeAt(index);
    this.form.markAsDirty();
    this.updateMessage();
  }

  protected buildForm(item: any, ignoreKeys?: string | string[]): FormControl | FormGroup | FormArray {
    // console.log("buildForm()", item);
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

  private buildFormGroup(object: any, ignoreKeys?: string | string[]): FormGroup {
    // console.log("buildFormGroup()", object);
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
    // console.log("buildFormControl()", item);
    return this.formBuilder.control({ value: item, disabled: !this.allowWrite });
  }

  private buildFormArray(array: any[], ignoreKeys?: string | string[]): FormArray {
    // console.log("buildFormArray()", array);
    var builder: any[] = [];
    for (let item of array) {
      var control = this.buildForm(item, ignoreKeys);
      if (control) {
        builder.push(control);
      }
    }
    return this.formBuilder.array(builder);
  }

}
