# Translations

The documentation is written in English. Translations are kept as PO files and are turned into pages at build time with [po4a](https://po4a.org/). Each language is a version of the `openems` component (e.g. `de`), so the theme can switch between the languages on every page.

```
l10n/po4a.cfg          list of translated pages and their output paths
l10n/po/<page>.pot     template per page (generated, do not edit)
l10n/po/<page>.de.po   German translation per page
l10n/de/antora.yml     German component version; runs po4a via the Antora Collector extension
l10n/nav.adoc          English source of the navigation of the translations
```

Translated are the user documentation (`modules/ROOT/pages/user`) and the additional pages of the Developer Guide (`modules/ROOT/pages/development`, without the existing overview, IDE, coding guidelines and documentation pages). The translations have one navigation with both parts; its English source is `l10n/nav.adoc`, the English site keeps using `modules/ROOT/nav.adoc`. Links from translated pages to pages that only exist in English point to the English version (`xref:latest@...`).

## Requirements

po4a (e.g. `apt install po4a` or `brew install po4a`).

## After changing English pages

Run in the `doc` directory:

```
po4a l10n/po4a.cfg
```

This updates the `.pot` and `.po` files. New or changed paragraphs are untranslated or marked as "fuzzy" in the `.po` files. Translate them with a PO editor (e.g. Poedit) or a text editor and remove the `fuzzy` flag. Untranslated and fuzzy paragraphs are shown in English.

## Adding a page

Add a line for the page to `l10n/po4a.cfg`, then run `po4a l10n/po4a.cfg`.

## Building

`buildAntoraDocs` (or `npx antora build/site.yml` in `doc`) runs `po4a --no-update l10n/po4a.cfg` through the Collector extension. This only writes the translated pages to `build/l10n/<lang>` and does not change the PO files.
