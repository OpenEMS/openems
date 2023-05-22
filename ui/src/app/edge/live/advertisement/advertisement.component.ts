import { AfterContentChecked, ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonSlides, ModalController } from '@ionic/angular';
import { AdvertWidgets } from 'src/app/shared/type/widget';
import { environment } from 'src/environments';
import { Edge, EdgeConfig, Service } from '../../../shared/shared';

@Component({
  selector: 'advertisement',
  templateUrl: './advertisement.component.html'
})

export class AdvertisementComponent implements OnInit, AfterContentChecked, OnDestroy {

  @Input() public advertWidgets: AdvertWidgets;

  @ViewChild('slides', { static: true }) public slides: IonSlides;

  public edge: Edge;
  public config: EdgeConfig;
  public environment = environment;
  public activeIndex: number;
  public title: string;
  public imageUrl: string = "assets/img/fems-app.png";

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
    private cdref: ChangeDetectorRef
  ) { }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }

  ngOnInit() {

    // Slide to random first view
    this.slides.getActiveIndex().then(index => {
      this.title = this.advertWidgets.list[index].title ?? environment.edgeShortName + ' - App';
    });

    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });

    // enables or disables nav buttons generally
    this.slides.length().then(length => {
      if (length > 1) {
        this.enableBtn = true;
        this.disablePrevBtn = true;
        this.disableNextBtn = false;
      }
    });
  }

  ngOnDestroy() {
    this.slides.stopAutoplay();
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
  }

  changeSlides() {
    this.slides.getActiveIndex().then((index: number) => {
      this.activeIndex = index;
    });
    this.slides.update().then(() => {
      this.title = this.advertWidgets.list[this.activeIndex].title ?? environment.edgeShortName + ' - App';
    });
  }

  changeNextPrevButtons() {

    // If more than one slide
    if (this.enableBtn) {
      this.slides.getSwiper().then(swiper => {
        if (swiper.isBeginning) {
          // Show only nextButton for first slide
          this.disablePrevBtn = true;
          this.disableNextBtn = false;
        } else if (swiper.isEnd) {
          // Show only previousButton for last slide
          this.disablePrevBtn = false;
          this.disableNextBtn = true;
        } else {
          this.disablePrevBtn = false;
          this.disableNextBtn = false;
        }
      });
    }
  }

  swipeNext() {
    this.slides.slideNext();
  }

  swipePrevious() {
    this.slides.slidePrev();
  }
}
