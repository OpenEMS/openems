import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { InstallationData } from '../../installation.component';

@Component({
  selector: CompletionComponent.SELECTOR,
  templateUrl: './completion.component.html'
})
export class CompletionComponent {

  private static readonly SELECTOR = "completion";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  constructor(private router: Router) { }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.router.navigate(["device", this.installationData.edge.id]);
  }

  public downloadProtocol() { }

}
