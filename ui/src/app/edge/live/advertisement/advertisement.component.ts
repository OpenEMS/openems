import { ActivatedRoute } from '@angular/router';
import { AdvertWidgets } from 'src/app/shared/type/widget';
import { Component, ViewChild, Input } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})

export class AdvertisementComponent {

  @Input() public advertWidgets: AdvertWidgets;

  @ViewChild('slides', { static: true }) slides: IonSlides;

  public edge: Edge;
  public config: EdgeConfig;

  private static readonly SELECTOR = "advertisement";

  public enableBtn: boolean = false;
  public disablePrevBtn: boolean = null;
  public disableNextBtn: boolean = null;

  slideOpts = {
    allowTouchMove: false,
    initialSlide: 0,
    preventClicks: false,
    preventClicksPropagation: false,
    speed: 5000,
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
    // enables or disables nav buttons generally
    if (this.advertWidgets.names.length > 1) {
      this.enableBtn = true;
      this.disablePrevBtn = true;
      this.disableNextBtn = false;
    } else {
      this.enableBtn = false;
    }
  }

  ngOnDestroy() {
    this.slides.stopAutoplay();
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
  }

  changeSlides() {
    this.slides.update();
  }

  changeNextPrevButtons() {
    // checks if slide is first/last element and sets the next/previous button accordingly
    if (this.enableBtn == true) {
      this.slides.getSwiper().then(swiper => {
        // not using isEnd/Beginning Promise because at 2 slides it sometimes returns false for both options
        let index = swiper.realIndex
        if (index == 0) {
          this.disablePrevBtn = true
          this.disableNextBtn = false
        } else if (index == this.advertWidgets.names.length - 1) {
          this.disablePrevBtn = false
          this.disableNextBtn = true
        } else {
          this.disablePrevBtn = false
          this.disableNextBtn = false
        }
      })
    }
  }

  swipeNext() {
    this.slides.slideNext()
  }

  swipePrevious() {
    this.slides.slidePrev()
  }
}
