// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Category } from '../../shared/category';

@Component({
  selector: ConfigurationLineSideMeterFuseComponent.SELECTOR,
  templateUrl: './configuration-line-side-meter-fuse.component.html',
})
export class ConfigurationLineSideMeterFuseComponent implements OnInit {

  private static readonly SELECTOR = 'configuration-line-side-meter-fuse';

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  protected form: FormGroup;
  protected fields: FormlyFieldConfig[];
  protected model;
  protected header: string;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {
    this.header = Category.toTranslatedString(this.ibn.lineSideMeterFuse.category, this.translate);
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
