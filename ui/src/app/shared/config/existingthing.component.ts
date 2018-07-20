import { Component, Input, OnChanges, ViewChildren, QueryList } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { ChannelComponent } from './channel.component';
import { Utils } from '../service/utils';
import { ConfigImpl } from '../edge/config';
import { Edge } from '../edge/edge';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';
import { ConfigImpl_2018_7 } from '../edge/config.2018.7';

@Component({
  selector: 'existingthing',
  templateUrl: './existingthing.component.html'
})
export class ExistingThingComponent implements OnChanges {

  public _edge: Edge = null;
  public thing = null;
  public meta = null;
  public role: Role = "guest";
  public config: ConfigImpl_2018_7 = null;
  public formPristine: boolean = true;
  public messages: { [channelId: string]: DefaultTypes.ConfigUpdate } = {};

  private stopOnDestroy: Subject<void> = new Subject<void>();

  // sets the flag if subthings should be shown, e.g. a Device of a Bridge
  @Input() public showSubThings: boolean = false;

  @Input() set edge(edge: Edge) {
    this.role = edge.role;
    this._edge = edge;
    edge.config.takeUntil(this.stopOnDestroy)
      .filter(edge => edge != null)
      .takeUntil(this.stopOnDestroy).subscribe(config => {
        if (edge.isVersionAtLeast('2018.8')) {
          console.error("ExistingThingComponent is not compatible with version > 2018.8");
          this.config = null;
        } else {
          this.config = <ConfigImpl_2018_7>config;
        }
      });
  }
  get edge(): Edge {
    return this._edge;
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
      this.edge.send(message);
    }
    this.messages = {};
    this.formPristine = true;
  }
}
