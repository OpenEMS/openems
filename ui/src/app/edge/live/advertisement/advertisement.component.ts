import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})
export class AdvertisementComponent {

  private static readonly SELECTOR = "advertisement";

  private edge: Edge = null;

  slideOpts = {
    initialSlide: 1,
    speed: 400
  };

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    })
  }
}
