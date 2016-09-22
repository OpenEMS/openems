import { OpenemsGuiPage } from './app.po';

describe('openems-gui App', function() {
  let page: OpenemsGuiPage;

  beforeEach(() => {
    page = new OpenemsGuiPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
