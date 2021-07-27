import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { InstallationData } from '../../installation.component';

@Component({
  selector: ProtocolCompletionComponent.SELECTOR,
  templateUrl: './protocol-completion.component.html'
})
export class ProtocolCompletionComponent implements OnInit {

  private static readonly SELECTOR = "protocol-completion";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  constructor() { }

  public ngOnInit(): void { }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.nextViewEvent.emit();
  }
}
