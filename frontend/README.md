# frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```
# Background Image Issue

The issue occurs because the pseudo-element is attached to :root (the <html> tag) using position: absolute. When the viewport is scaled down extremely small (e.g., below 200px), any content overflow or layout wrapping can cause the calculated height of :root to change. Combined with an ultra-high-resolution image (5640x2460), the browser struggles to scale it properly, resulting in white gaps.

By changing position: absolute to position: fixed, the pseudo-element will always anchor itself to the viewport (the screen) instead of the document root, no matter how much you zoom or resize.

```css
:root::before {
    content: "";
    position: fixed; /* replace absolute with fixed */
    inset: 0;
    background-image: url("LoginBackground.jpg");
    background-size: cover;
    background-position: center;
    filter: blur(2px);
    transform: scale(1.05); /* 防止模糊后四周露白 */
}
```