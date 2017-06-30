import { Ng20170527Page } from './app.po';

describe('ng20170527 App', () => {
  let page: Ng20170527Page;

  beforeEach(() => {
    page = new Ng20170527Page();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
