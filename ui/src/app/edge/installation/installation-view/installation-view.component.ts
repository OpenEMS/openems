import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: InstallationViewComponent.SELECTOR,
  templateUrl: './installation-view.component.html'
})
export class InstallationViewComponent implements OnInit {

  private static readonly SELECTOR = "installation-view";

  @Input() public iconName: string;
  @Input() public header: string;
  @Input() public subtitle: string;
  @Input() public fields: FormlyFieldConfig[];

  @Input() public isWaiting: boolean = false;
  @Input() public isFirstView: boolean = false;
  @Input() public isLastView: boolean = false;

  @Output() public previousClicked: EventEmitter<any> = new EventEmitter();
  @Output() public nextClicked: EventEmitter<any> = new EventEmitter();
  public labelToShow: { label: string, icon: string } | null = null;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {

    if (this.isWaiting) {
      this.labelToShow = {
        label: this.translate.instant('INSTALLATION.LOAD'),
        icon: 'hourglass-outline'
      };
    } else if (this.isLastView) {
      this.labelToShow = {
        label: this.translate.instant('INSTALLATION.COMPLETE'),
        icon: 'checkmark-done-outline1'
      };
    } else {
      this.labelToShow = {
        label: this.translate.instant('INSTALLATION.NEXT'),
        icon: 'arrow-forward-outline'
      };
    }
  }
  public keyPressed(event) {
    if (event.key === 'Enter') this.nextClicked.emit();
  }
}