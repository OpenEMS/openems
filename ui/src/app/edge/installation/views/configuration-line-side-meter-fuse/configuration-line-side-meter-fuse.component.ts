import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

@Component({
  selector: ConfigurationLineSideMeterFuseComponent.SELECTOR,
  templateUrl: './configuration-line-side-meter-fuse.component.html'
})
export class ConfigurationLineSideMeterFuseComponent implements OnInit {

  private static readonly SELECTOR = "configuration-line-side-meter-fuse";

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;
  public header: string;

  constructor() { }

  public ngOnInit() {
    this.header = this.ibn.lineSideMeterFuseTitle;
    this.form = new FormGroup({});
    this.fields = this.ibn.getLineSideMeterFuseFields();
    this.model = this.ibn.lineSideMeterFuse ?? {};
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.ibn.lineSideMeterFuse = this.model;
    this.nextViewEvent.emit(this.ibn);
  }
}
