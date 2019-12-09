import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: CorrentlyModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class CorrentlyModalComponent {


  private static readonly SELECTOR = "corrently-modal";

  public startDate = null;
  public endDate = null;
  public zipCode = null;
  public city = null;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
  }

  getStartDate($event) {
    this.startDate = $event;
  }

  getEndDate($event) {
    this.endDate = $event;
  }

  getZipCode($event) {
    this.zipCode = $event;
  }

  getCity($event) {
    this.city = $event;
  }

}