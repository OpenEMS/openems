import { ActivatedRoute } from '@angular/router';
import { Component, ViewChild } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})


export class AdvertisementComponent {

  @ViewChild('slider', { static: true }) slides: IonSlides;

  private static readonly SELECTOR = "advertisement";

  public edge: Edge = null;
  public disablePrevBtn = null;
  public disableNextBtn = null;
  private config: EdgeConfig = null;


  slideOpts = {
    initialSlide: 1,
    speed: 5000
  };

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.config = config;
      });
    })
    this.slides.length().then(length => {
      if (length > 1) {
        this.disablePrevBtn = true;
        this.disableNextBtn = false;
      } else {
        this.disablePrevBtn = true;
        this.disableNextBtn = true;
      }
    })
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
  }

  isLastSlide() {
    let isBeginning = this.slides.isBeginning();
    let isEnd = this.slides.isEnd();

    Promise.all([isBeginning, isEnd]).then(data => {
      data[0] ? this.disablePrevBtn = true : this.disablePrevBtn = false;
      data[1] ? this.disableNextBtn = true : this.disableNextBtn = false;
    });
  }

  swipeNext() {
    console.log("config", this.config)
    this.slides.slideNext()
  }

  swipePrevious() {
    this.slides.slidePrev()
  }
}
