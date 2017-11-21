import { Component, Input, OnChanges, ViewChildren, QueryList } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { ChannelComponent } from './channel.component';
import { Utils } from '../service/utils';
import { ConfigImpl } from '../device/config';
import { Device } from '../device/device';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';

@Component({
  selector: 'existingthing',
  templateUrl: './existingthing.component.html'
})
export class ExistingThingComponent implements OnChanges {

  public _device: Device = null;
  public thing = null;
  public meta = null;
  public role: Role = "guest";
  public config: ConfigImpl = null;
  public formPristine: boolean = true;
  public messages: { [channelId: string]: DefaultTypes.ConfigUpdate } = {};

  private stopOnDestroy: Subject<void> = new Subject<void>();

  // sets the flag if subthings should be shown, e.g. a Device of a Bridge
  @Input() public showSubThings: boolean = false;

  @Input() set device(device: Device) {
    this.role = device.role;
    this._device = device;
    device.config.takeUntil(this.stopOnDestroy)
      .filter(device => device != null)
      .takeUntil(this.stopOnDestroy).subscribe(config => {
        this.config = config;
      });
  }
  get device(): Device {
    return this._device;
  }

  @Input() public thingId: string = null;

  @ViewChildren(ChannelComponent)
  private channelComponentChildren: QueryList<ChannelComponent>;

  constructor(
    public utils: Utils,
    private formBuilder: FormBuilder) { }

  /**
   * Receive form.pristine state
   */
  ngAfterViewInit() {
    this.channelComponentChildren.forEach(channelComponent => {
      channelComponent.message
        .takeUntil(this.stopOnDestroy)
        .subscribe((message) => {
          if (message == null) {
            delete this.messages[channelComponent.channelId];
            this.formPristine = true;
          } else {
            // set pristine flag
            let pristine = true;
            this.channelComponentChildren.forEach(channelComponent => {
              pristine = pristine && channelComponent.form.pristine;
            });
            this.formPristine = pristine;
            // store message
            this.messages[channelComponent.channelId] = message;
          }
        });
    });
  }

  ngOnChanges() {
    if (this.config != null && this.thingId != null && this.thingId in this.config.things) {
      this.thing = this.config.things[this.thingId];
      this.meta = this.config.meta[this.thing.class];
    }
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  public save() {
    for (let message of this.utils.values(this.messages)) {
      this.device.send(message);
    }
    this.messages = {};
    this.formPristine = true;
  }
}
