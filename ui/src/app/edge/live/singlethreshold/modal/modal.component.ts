import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

type state = 'ON' | 'OFF'
type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'PRODUCTION' | 'OTHER'

@Component({
  selector: SinglethresholdModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

  private static readonly SELECTOR = "singlethreshold-modal";

  public selectedText: string = null;

  @Input() public edge: Edge;
  @Input() public mode: mode;
  @Input() public inputMode: inputMode;
  @Input() public threshold: number;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
  ) { }

  ngOnInit() {

  }

  codeSelected() {
    switch (this.inputMode) {
      case "SOC":
        this.selectedText = "Ladezustand";
        break;
      case "GRIDSELL":
        this.selectedText = "Netzeinspeisung";
        break;
      case "PRODUCTION":
        this.selectedText = "Production";
        break;
    }
  }

  updateMode(event: CustomEvent) {
    this.mode = event.detail.value;
  }

  getState(): state {
    if (this.edge != null) {
      if (this.mode == 'OFF') {
        return 'OFF'
      } else if (this.mode == 'ON') {
        return 'ON'
      } else if (this.mode == 'AUTOMATIC') {
        switch (this.inputMode) {
          case 'SOC': {
            if (this.threshold < this.edge.currentData.value.channel['_sum/EssSoc']) {
              return 'ON';
            } else if (this.threshold > this.edge.currentData.value.channel['_sum/EssSoc']) {
              return 'OFF';
            } else {
              return 'OFF';
            }
          }
          case 'GRIDSELL': {
            if (this.edge.currentData.value.channel['_sum/GridActivePower'] * -1 >= 0) {
              if (this.threshold < this.edge.currentData.value.channel['_sum/GridActivePower'] * -1) {
                return 'ON';
              } else if (this.threshold > this.edge.currentData.value.channel['_sum/GridActivePower'] * -1) {
                return 'OFF';
              }
            } else {
              return 'OFF';
            }
          }
          case 'PRODUCTION': {
            if (this.threshold < this.edge.currentData.value.channel['_sum/ProductionActivePower']) {
              return 'ON';
            } else if (this.threshold > this.edge.currentData.value.channel['_sum/ProductionActivePower']) {
              return 'OFF';
            } else {
              return 'OFF';
            }
          }
        }
      }
    }
  }

  dismiss() {
    let values = {
      mode: this.mode,
      inputMode: this.inputMode,
      threshold: this.threshold,
    }
    this.modalCtrl.dismiss(values)
  }
}