import { Component, Input, Output, OnChanges, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import * as moment from 'moment';

import { DefaultMessages } from '../../../shared/service/defaultmessages';
import { Utils } from '../../../shared/service/utils';
import { ConfigImpl } from '../../../shared/device/config';
import { Device } from '../../../shared/device/device';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { Role, ROLES } from '../../../shared/type/role';

@Component({
  selector: 'channel',
  templateUrl: './channel.component.html'
})
export class ChannelComponent implements OnChanges, OnDestroy {

  public form: FormGroup = null;
  public meta = null;
  public type: string;
  public specialType: "simple" | "ignore" | "boolean" | "selectNature" = "simple";

  private stopOnDestroy: Subject<void> = new Subject<void>();

  @Input() public thingId: string = null;
  @Input() public channelId: string = null;
  @Input() public config: ConfigImpl = null;
  @Input() public role: Role = ROLES.guest;

  @Output() public message: Subject<DefaultTypes.ConfigUpdate> = new Subject<DefaultTypes.ConfigUpdate>();

  constructor(
    public utils: Utils,
    private formBuilder: FormBuilder) { }

  ngOnChanges() {
    if (this.config != null && this.thingId != null && this.thingId in this.config.things && this.channelId != null) {
      let thingConfig = this.config.things[this.thingId];
      let clazz = thingConfig.class;
      if (clazz instanceof Array) {
        return;
      }
      let thingMeta = this.config.meta[clazz];
      let channelMeta = thingMeta.channels[this.channelId];
      this.meta = channelMeta;
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
        // case 'Ess':
        // case 'Meter':
        // case 'RealTimeClock':
        // case 'Charger':
        //   this.specialType = 'selectNature';
        //   this.type = this.meta.type + 'Nature';
        //   break;
        case 'JsonArray':
        case 'JsonObject':
          this.specialType = 'ignore';
          break;
        default:
          if (metaType in this.config.meta) {
            // this is actually another thing -> ignore here
            let otherThingMeta = this.config.meta[metaType];
            console.warn("Ignore embedded thing", otherThingMeta);
            this.specialType = 'ignore';
          } else if (this.config.meta instanceof Array) {
            // this is a DeviceNature id
            this.specialType = 'selectNature';
            this.type = this.meta.type + 'Nature';
          } else {
            console.warn("Unknown type: " + this.meta.type, this.meta);
            this.type = 'string';
          }
      }
      // console.log(this.thingId, this.channelId, thingConfig, channelMeta, this.config, this.role);

      // get value or default value
      let value = thingConfig[this.channelId];
      if (value == null) {
        value = channelMeta.defaultValue;
      }

      // build form
      this.form = this.buildFormGroup({ channelValue: value });
      // console.log(this.form);

      // subscribe to form changes and build websocket message
      this.form.valueChanges
        .takeUntil(this.stopOnDestroy)
        .map(data => data["channelValue"])
        .subscribe(value => {
          this.message.next(DefaultMessages.configUpdate(this.thingId, this.channelId, value));
        });


      // this.meta = this.config.meta[thingConfig.class];
      // this.form = this.buildFormGroup(thingConfig);
      // console.log(thingConfig, this.meta, this.form);
    }
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
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

  private buildFormGroup(object: any, ignoreKeys?: string | string[]): FormGroup {
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

}
