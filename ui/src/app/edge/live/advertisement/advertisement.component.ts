import { AfterContentChecked, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { AdvertWidgets } from 'src/app/shared/type/widget';
import { environment } from 'src/environments';
import { register } from 'swiper/element/bundle';
import { Service } from '../../../shared/shared';

register();

@Component({
  selector: 'advertisement',
  templateUrl: './advertisement.component.html',
})

export class AdvertisementComponent implements OnInit, AfterContentChecked, OnDestroy {

  @Input() public advertWidgets: AdvertWidgets;

  @ViewChild('swiper') public swiperRef: ElementRef | undefined;

  public title: string;
  public imageUrl: string = "assets/img/fems-app.png";

  protected enableBtn: boolean = false;
  protected disablePrevBtn: boolean | null = null;
  protected disableNextBtn: boolean | null = null;

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
    private cdref: ChangeDetectorRef,
  ) { }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }

  ngOnInit() {
    this.setSlideConfig();
  }

  ngOnDestroy() {
    this.swiperRef?.nativeElement.swiper.autoplay.stop();
  }

  /**
   * Sets the slide config like title and disables prev and next button
   */
  protected setSlideConfig() {

    // Slide to random first view
    const index = this.swiperRef?.nativeElement.swiper.activeIndex ?? 0;
    this.title = this.advertWidgets.list[index].title ?? environment.edgeShortName + ' - App';

    this.changeNextPrevButtons();
  }

  private changeNextPrevButtons() {

    // enables or disables nav buttons generally
    const length = this.swiperRef?.nativeElement.swiper.slides.length ?? 0;
    this.enableBtn = length > 1;

    // If more than one slide
    if (this.enableBtn) {
      const isBeginning = this.swiperRef?.nativeElement.swiper.isBeginning || false;
      const isEnd = this.swiperRef?.nativeElement.swiper.isEnd || false;
      this.disablePrevBtn = isBeginning;
      this.disableNextBtn = isEnd;
    }
  }

  protected swipeNext() {
    this.swiperRef?.nativeElement.swiper.slideNext();
  }

  protected swipePrevious() {
    this.swiperRef?.nativeElement.swiper.slidePrev();
  }
}
