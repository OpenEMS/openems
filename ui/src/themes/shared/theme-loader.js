/**
 * Gets the valid theme
 * 
 * @param {*} theme the stored theme
 * @returns either "light" or "dark"
 */
function getValidTheme(theme) {
  if (theme === "dark" || theme === "light") {
    return theme;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light"
}

/**
 * Sets theme on init, before angular app inits
 */
function setTheme() {
  const stored = localStorage.getItem('THEME');
  const theme = getValidTheme(stored);
  document.documentElement.setAttribute('data-theme', theme);

  const style = document.createElement('style');
  const bg = localStorage.getItem('THEME_COLOR');
  style.innerHTML = `
        html, body {
          background-color: ${bg};
          height: 100%;
        }

        /* Optional: hide app briefly if theme is still resolving */
        html:not([data-theme]) body {
          visibility: hidden;
        }
      `;
  document.head.appendChild(style);
};

setTheme();
