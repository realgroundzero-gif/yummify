---
name: Küche & Vorrat M3
colors:
  surface: '#fdf8f5'
  surface-dim: '#ded9d6'
  surface-bright: '#fdf8f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f8f3f0'
  surface-container: '#f2edea'
  surface-container-high: '#ece7e4'
  surface-container-highest: '#e6e2df'
  on-surface: '#1c1b1a'
  on-surface-variant: '#57423b'
  inverse-surface: '#32302e'
  inverse-on-surface: '#f5f0ed'
  outline: '#8a726a'
  outline-variant: '#dec0b7'
  surface-tint: '#a23e18'
  primary: '#9f3c16'
  on-primary: '#ffffff'
  primary-container: '#bf542c'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb59c'
  secondary: '#2d6a46'
  on-secondary: '#ffffff'
  secondary-container: '#aeeec1'
  on-secondary-container: '#316e4a'
  tertiary: '#715644'
  on-tertiary: '#ffffff'
  tertiary-container: '#8c6f5b'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59c'
  on-primary-fixed: '#390c00'
  on-primary-fixed-variant: '#822801'
  secondary-fixed: '#b0f1c4'
  secondary-fixed-dim: '#95d5a9'
  on-secondary-fixed: '#00210f'
  on-secondary-fixed-variant: '#0f5130'
  tertiary-fixed: '#ffdcc5'
  tertiary-fixed-dim: '#e3c0a8'
  on-tertiary-fixed: '#2a1709'
  on-tertiary-fixed-variant: '#5a4230'
  background: '#fdf8f5'
  on-background: '#1c1b1a'
  surface-variant: '#e6e2df'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 57px
    fontWeight: '600'
    lineHeight: 64px
    letterSpacing: -0.25px
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 45px
    fontWeight: '600'
    lineHeight: 52px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: 0px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: 0.15px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.75rem
  margin: 1rem
  margin-expanded: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system translates the Material You (Material 3) expressive philosophy into an organic, tactile culinary assistant. Designed specifically for an Android meal-planning and recipe-management tool syncing bi-directionally with Notion, the aesthetic merges the grounding comfort of a traditional cookbook with the dynamic, adaptive responsiveness of modern Android UI.

### Design Direction: Modern Organic Material (M3 Expressive)
- **Personality:** Grounded, nourishing, mindful, dependable, and systematically clear.
- **Visual Tenets:**
  - **Tonal Depth over Harsh Shadows:** Surfaces elevate through tinted container hierarchies (Surface Container Lowest to Highest) rather than sharp drop shadows.
  - **Culinary Organic Harmony:** Warm baked terracotta (#C85A32) grounds focal calls-to-action, paired with deep garden sage (#2E6B47) and herb accents for tags, nutritional balance indicators, and Notion sync states.
  - **Generous Tactility:** Large 16px–28px rounded corners invite touch across one-handed mobile layouts, honoring standard Material 3 ergonomics.
  - **Localization Readiness (German UI):** Layouts account for longer compound words ("Wochenend-Speiseplan", "Zubereitungszeit", "Einkaufsliste") by avoiding rigid single-line constraints and providing flexible horizontal chip scrollers and wrapping typography slots.

## Colors

The palette adopts an earthy, culinary-focused Material 3 color mapping. Dynamic tonal values ensure accessibility, contrast compliance (WCAG AA/AAA), and visual cohesion across culinary categories.

### Light Palette (Standard Canvas)
- **Primary (`#C85A32` - Terracotta):** Used for focal interactive elements, active navigation pills, primary Floating Action Buttons (FAB), and confirmed state toggles.
- **On-Primary (`#FFFFFF`):** High-contrast text/iconography over terracotta surfaces.
- **Primary Container (`#FFDBCF`):** Soft, warm blush container for active filter chips and highlighted ingredient cards.
- **On-Primary Container (`#3A0B00`):** Rich espresso-terracotta for accessible text on primary containers.
- **Secondary (`#2E6B47` - Tiefes Salbei):** Nutritional tags, Notion synchronized badges, dietary indicators (vegan, vegetarisch), and secondary interactive elements.
- **Secondary Container (`#B3F0C5`):** Soft sage base for grocery list badges and completed recipe steps.
- **On-Secondary Container (`#00210E`):** High-legibility deep forest tone.
- **Tertiary (`#765B48` - Geröstete Mandel):** Prep time indicators, pantry category badges, and subtle informational notes.
- **Tertiary Container (`#FFDCC2`):** Low-contrast warm wash for inactive/idle group blocks.
- **Surface (`#FDF8F5` - Warm Cream Canvas):** Clean culinary background tint evoking linen and unbleached parchment.
- **Surface Container Lowest (`#FFFFFF`):** Pure white for elevated popovers and camera recipe scanning viewports.
- **Surface Container Low (`#F8F2EE`):** Default card background for list rows and meal day columns.
- **Surface Container (`#F2ECE8`):** Main deck background, docked SearchBar, and grouped settings.
- **Surface Container High (`#ECE6E2`):** Elevated cards, sheet drawers, and dialog backgrounds.
- **Surface Container Highest (`#E6E1DC`):** Inactive segmented buttons, disabled inputs, and subtle separators.
- **Surface Variant (`#F5DED6`):** Input field fills and progress track backgrounds.
- **Outline (`#85736E`):** Outlined buttons, unselected chip borders, and precise Notion sync grid dividers.
- **Outline Variant (`#D8C2BB`):** Subdued hairpins and card divider lines.
- **Error (`#BA1A1A`):** Ingredient stockout alerts, failed Notion sync warnings, and destructive actions.

### Dark Surface Mapping (Adaptive Dark Mode)
- **Surface (`#1C1B1F`):** Deep charcoal night canvas.
- **Surface Container (`#262428`):** Elevated dark cards.
- **Primary (`#FFB59D`):** Desaturated high-luminance terracotta for night legibility.
- **Secondary (`#98D7AA`):** Crisp luminescent mint sage for recipe timers.

## Typography

The type system uses **Plus Jakarta Sans**, providing modern geometric cleanliness aligned with Google's modern Android typography guidelines while offering open apertures and warm counterforms well suited to food media.

### Implementation Principles
- **German Language Pacing:** German nouns and titles are long (e.g., *Zubereitungshinweise*, *Einkaufszettel*, *Nährwertdeklaration*). Never fix line-heights to rigid pixel boxes that clip descenders or prevent two-line wrapping on `title-md` and `title-sm`.
- **Display & Headlines:** Used for recipe names, collection hero headers, and calendar week overviews. Heavy 600–700 weights ensure visual authority over rich food photography.
- **Body:** Calibrated line-heights (1.5x ratio) guarantee legibility on dirty kitchen tablet/mobile screens mounted at arm's length.
- **Labels:** Crisp 500–600 weight tracking for chip indicators, cooking step timers ("15 Min."), and Notion sync state tags ("Zuletzt synchronisiert: vor 3 Min.").

## Layout & Spacing

Layout adheres to an 8dp/4dp strict incremental grid native to Android Material 3, adapting dynamically across standard smartphone screens (compact) and foldable/tablet kitchen displays (expanded).

### Breakpoints & Adaptive Layout Rules
- **Compact (Mobile Phones, &lt; 600dp width):**
  - **Outer Canvas Margins:** `1rem` (16px).
  - **Columns:** 4-column layout for recipe card feeds, or single-column vertical flows for step-by-step cooking modes.
  - **Bottom Navigation:** Uses standard 80dp tall `NavigationBar` with 4 to 5 main targets (Rezepte, Wochenplan, Vorrat, Notion Sync).
- **Medium & Expanded (Foldables & Tablets, 600dp+ width):**
  - **Outer Canvas Margins:** `1.5rem` (24px).
  - **Columns:** 8 to 12 columns. Transition the bottom bar into a leading `NavigationRail` (80dp wide) anchored to the left hand.
  - **Master-Detail Flow:** Left pane holds the recipe library or meal planner calendar; right pane hosts the full Notion ingredient table and cooking instructions.

### Vertical Rhythm
- Inter-card gap inside meal-prep feeds: `space-md` (16px).
- Internal card padding: `space-md` (16px) for simple items; `space-lg` (24px) for prominent featured recipe cards.
- Bottom clearance for FAB & Navigation Bar: safe area inset + 96px to prevent floating control overlap with culinary checklist items.

## Elevation & Depth

This system eliminates artificial drop shadows in favor of **Tonal Surface Tinting**, the foundational depth principle of Material 3.

### Surface Tonal Hierarchy
1. **Level 0 (Flat / Canvas):** `Surface` (#FDF8F5). Background behind scrolling lists and category grids.
2. **Level 1 (Card Default / Inactive Container):** `Surface Container Low` (#F8F2EE). Used for standard unselected recipe cards, ingredient list rows, and meal day columns. No drop shadow; separation occurs via tonal contrast and optional `Outline Variant` hairpins.
3. **Level 2 (Active Interactive Cards & Docked SearchBar):** `Surface Container` (#F2ECE8). Used for the docked top `SearchBar`, active day columns, and hovered meal slots.
4. **Level 3 (Elevated Sheets & Floating Action Buttons):** `Surface Container High` (#ECE6E2) with ambient diffuse shadow: `box-shadow: 0px 4px 12px rgba(46, 30, 20, 0.08)`. Tinted with a warm organic brown undertone rather than pure black.
5. **Level 4 (Modal Drawers & Dialogs):** `Surface Container Highest` (#E6E1DC) with `box-shadow: 0px 8px 24px rgba(46, 30, 20, 0.12)`. Applied to full-screen ingredient substitution dialogs and Notion sync conflict sheets.

## Shapes

The design system embraces the expressive curvature of Material 3, relying heavily on `rounded-2xl` (16px) up to `rounded-3xl` (24px–28px) for large surface envelopes.

### Shape Scale Rules
- **Extra Small (4px / 0.25rem):** Progress bars, Notion database tag pills.
- **Small (8px / 0.5rem):** Inline badges, preparation step numbers, and segmented button inner boundaries.
- **Medium (12px / 0.75rem):** Text input fields, dropdown menus, and recipe checklist toggles.
- **Large (16px / 1rem - `rounded-lg`):** Standard recipe cards, meal planner slot containers, and Notion property sheets.
- **Extra Large (24px to 28px - `rounded-xl` to `rounded-3xl`):** Hero recipe header images, docked SearchBar, bottom sheet modals, and primary FABs.
- **Full Pill (`rounded-full`):** Filter chips, active bottom navigation indicator capsules, and segmented pill buttons.

## Components

### 1. Docked SearchBar & Notion Sync Anchor
- **Anatomy:** Height 56px, fully pill-shaped (`rounded-full`) or 28px rounded container. Sits at the top of the recipe feed.
- **Background:** `Surface Container` (#F2ECE8).
- **Leading Icon:** Search magnifier (24px) in `Outline` (#85736E).
- **Placeholder:** "Rezepte, Zutaten oder Tags durchsuchen…" in `body-md` muted.
- **Trailing Element:** Notion Sync Avatar indicator. Features the Notion workspace icon surrounded by an animated or static status ring:
  - Synchronized: Small 8px dot in `Secondary` (#2E6B47).
  - Syncing/In Progress: Pulsing ring in `Primary` (#C85A32).
  - Conflict/Offline: Alert dot in `Error` (#BA1A1A).

### 2. Recipe Cards (M3 Elevated & Filled)
- **Filled Card:** Background `Surface Container Low` (#F8F2EE), no border, corner radius 20px. Features top image slot (aspect ratio 16:9 or 4:3), padding 16px, headline in `title-md`, prep time label in `Tertiary` (#765B48), and secondary chip metadata ("25 Min.", "Vegetarisch").
- **Elevated Card:** Background `Surface Container High` (#ECE6E2), subtle warm ambient shadow (`0px 4px 12px rgba(46, 30, 20, 0.08)`), corner radius 24px. Used for the designated "Rezept des Tages" (Recipe of the Day) or active meal being prepared.

### 3. M3 Filter Chips
- **Inactive State:** `Surface Container Low` fill, 1px border in `Outline Variant` (#D8C2BB), label in `label-lg` `On-Surface`, height 32px, `rounded-full`.
- **Selected State:** `Primary Container` (#FFDBCF) fill, borderless, text in `On-Primary Container` (#3A0B00). Includes leading 18px checkmark icon (`ic_check`).
- **Scroll Behavior:** Horizontal scroll track with 8px gaps and edge-fade gradient over the `Surface` canvas.

### 4. Floating Action Button (FAB)
- **Primary Standard FAB:** 56x56dp rounded square with 16px radius (`rounded-2xl`). Fill `Primary Container` (#FFDBCF) with icon in `On-Primary Container` (#3A0B00), or `Primary` (#C85A32) with `On-Primary` (#FFFFFF) depending on context.
- **Extended FAB:** Height 56px, `rounded-2xl`, expands on feed idle: Leading `ic_add` icon + "Neues Rezept" label. Collapses to 56x56dp icon-only on downward scroll.

### 5. NavigationBar (M3 Bottom Bar)
- **Container:** Height 80dp, background `Surface Container` (#F2ECE8), 0px elevation, subtle top divider in `Outline Variant`.
- **Active Indicator:** Pill capsule (64px width, 32px height) in `Primary Container` (#FFDBCF).
- **Active Icon:** Tinted in `On-Primary Container` (#3A0B00).
- **Inactive Targets:** Clean outline icon and `label-md` label in `On-Surface Variant` (#504541).
- **Item Labels (German):**
  - "Rezepte" (Book icon)
  - "Wochenplan" (Calendar/Meal icon)
  - "Einkauf" (List/Cart icon)
  - "Vorrat" (Pantry jar icon)

### 6. Interactive Form Controls (Inputs, Checkboxes & Switches)
- **Text Input Fields:** Filled M3 style with bottom active rule or outlined with 12px corner radius. Container `Surface Variant` (#F5DED6), active focus line and label in `Primary` (#C85A32).
- **Recipe Step Checkboxes:** 20px rounded squares with 4px corner radius. When checked, filled with `Secondary` (#2E6B47) sage, triggering strike-through styling on the ingredient item.
- **Switches (M3 Large Thumb):** Track length 52px, track height 32px, pill-shaped. Inactive thumb 16px; Active thumb expands to 24px with checkmark glyph inside, colored `Primary` with `Primary Container` track.

### 7. Notion Database Properties Inspector (Specialized Component)
- Embedded bottom sheet or card module showing Notion sync details:
  - "Datenbank: Hauptrezepte (Notion)" with clean pill tag.
  - Two-way sync status button ("Jetzt synchronisieren").
  - Field mappings shown as structured badge rows: `Zutaten (Multi-Select)`, `Kalorien (Number)`, `Quelle (URL)`.