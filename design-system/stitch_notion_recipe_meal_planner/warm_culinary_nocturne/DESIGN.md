---
name: Warm Culinary Nocturne
colors:
  surface: '#151311'
  surface-dim: '#151311'
  surface-bright: '#3b3936'
  surface-container-lowest: '#0f0e0c'
  surface-container-low: '#1d1b19'
  surface-container: '#211f1d'
  surface-container-high: '#2c2a27'
  surface-container-highest: '#373432'
  on-surface: '#e7e1de'
  on-surface-variant: '#dbc1b8'
  inverse-surface: '#e7e1de'
  inverse-on-surface: '#32302e'
  outline: '#a38c84'
  outline-variant: '#55433c'
  surface-tint: '#ffb59a'
  primary: '#ffb59a'
  on-primary: '#5a1b00'
  primary-container: '#f08c65'
  on-primary-container: '#6b2606'
  inverse-primary: '#984726'
  secondary: '#97d3bd'
  on-secondary: '#00382b'
  secondary-container: '#145342'
  on-secondary-container: '#89c4af'
  tertiary: '#ebc16d'
  on-tertiary: '#402d00'
  tertiary-container: '#c8a151'
  on-tertiary-container: '#4f3800'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbce'
  primary-fixed-dim: '#ffb59a'
  on-primary-fixed: '#370d00'
  on-primary-fixed-variant: '#793010'
  secondary-fixed: '#b2efd9'
  secondary-fixed-dim: '#97d3bd'
  on-secondary-fixed: '#002118'
  on-secondary-fixed-variant: '#105040'
  tertiary-fixed: '#ffdea3'
  tertiary-fixed-dim: '#ebc16d'
  on-tertiary-fixed: '#261900'
  on-tertiary-fixed-variant: '#5d4200'
  background: '#151311'
  on-background: '#e7e1de'
  surface-variant: '#373432'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
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
    lineHeight: 28px
    letterSpacing: 0px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
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
  gutter-tablet: 1.5rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system establishes a warm, intimate, and tactile dark-mode culinary environment tailored for kitchen efficiency and mindful meal management. Balancing the organic vitality of cooking with the systematic rigor of Material Design 3, it pairs deep, warm-tinted obsidian foundations with luminous roasted terracotta accents. 

Targeting culinary enthusiasts, organized home cooks, and pantry managers, the aesthetic conveys calm precision, warmth, and appetite-stimulating comfort. Rather than harsh digital neon or stark cool gray, surfaces evoke oiled cast iron, charred cedar, and stone countertops under ambient kitchen pendant lighting. Visual layers prioritize scannability while hands are busy, offering effortless contrast, tactile tap zones, and clear hierarchical boundaries.

## Colors
The color architecture relies on a specialized dark-mode Material Design 3 surface progression tuned to warm undertones rather than neutral grays.

- **Primary (`#F08C65`)**: Luminous roasted terracotta tuned specifically for dark backgrounds, providing accessible contrast while preserving the appetizing punch of brick oven embers and paprika. Used for primary FABs, prominent action states, and key navigation highlights.
- **Secondary (`#82BDA8`)**: Fresh herb mint / sage green. Serves as a botanical counterweight, signifying fresh produce, stocked inventory statuses, active timers, and dietary tags.
- **Tertiary (`#E8BE6B`)**: Warm honey / Dijon gold. Used for pantry alerts, expiring-soon items, culinary tips, and rating badges.
- **Surfaces & Layers**:
  - `surface`: `#141211` (Deep OLED warm-charcoal base)
  - `surface-dim`: `#0F0E0D`
  - `surface-container-lowest`: `#100E0D`
  - `surface-container-low`: `#1D1B1A`
  - `surface-container`: `#211F1D`
  - `surface-container-high`: `#2C2927`
  - `surface-container-highest`: `#373432`
- **Text & Content**:
  - `on-surface`: `#ECE0DB` (Bone white with warm undertone for high legibility without visual fatigue)
  - `on-surface-variant`: `#B5A8A2` (Muted wheat for secondary ingredients, metadata, and hints)
  - `outline`: `#524C48` (Low-contrast separation for list partitions and inactive borders)
  - `outline-variant`: `#3C3734`

## Typography
Plus Jakarta Sans delivers geometric precision with humane, approachable warmth. Its open counters and clean terminal shapes make it exceptionally legible when scanning recipe instructions, grocery checklist items, and ingredient weights at arm's length on kitchen counters.

Weights are paired intentionally: semibold (600) reinforces structured visual anchor points across headlines and action triggers, while regular (400) maintains comfortable, breathing rhythm throughout multi-step preparation guides and nutrition tables.

## Layout & Spacing
The layout follows an adaptive fluid-grid framework strictly aligned to an 8dp baseline grid (with 4dp sub-steps for compact ingredient item lists and density chips):

- **Mobile (<600dp)**: 4 columns, 16px (`1rem`) gutters, 16px (`1rem`) edge margins. Vertical stacking prioritizes step-by-step cooking progress, thumb-reachable floating bottom sheets, and swipeable day cards.
- **Tablet (600dp–1024dp)**: 8 columns, 24px (`1.5rem`) gutters, 24px (`1.5rem`) margins. Shifts to a balanced dual-pane experience—meal planner or grocery list on the leading pane, active recipe detail or ingredient stock overview on the trailing pane.
- **Desktop / Smart Display (>1024dp)**: 12 columns with a max content container width of 1280px, 24px (`1.5rem`) gutters, and 40px (`2.5rem`) outer margin margins. Accommodates side-by-side multi-day meal calendars, detailed nutritional charts, and comprehensive pantry categorization matrices.

## Elevation & Depth
In alignment with Material 3 dark-mode specifications, depth is primarily conveyed through **Tonal Surface Layering** rather than heavy cast drop shadows:

- **Level 0 (Base)**: `surface` (`#141211`) for the overarching backdrop and canvas.
- **Level 1 (Resting Cards & Lists)**: `surface-container-low` (`#1D1B1A`) with an optional 1px hairline border of `outline-variant` (`#3C3734`) for soft structural delineation.
- **Level 2 (Active Cards & Toolbars)**: `surface-container` (`#211F1D`) with subtle warm amber-tinted ambient shadow (`box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4), 0 1px 2px rgba(240, 140, 101, 0.04)`).
- **Level 3 (Modals, Navigation Drawers & Floating Bottom Sheets)**: `surface-container-high` (`#2C2927`) combined with an intensified diffuse shadow (`0 8px 32px rgba(0, 0, 0, 0.6)`).
- **Level 4 (Floating Action Buttons & Tooltips)**: `surface-container-highest` (`#373432`) or solid `primary` (`#F08C65`) with dynamic elevation states that brighten on hover/focus.

## Shapes
Following Material 3’s expressive curve spectrum:
- **Small elements (8px / `rounded-md`)**: Checkboxes, text input containers, compact quantity badges, and context menu items.
- **Medium elements (12px to 16px / `rounded-lg`)**: Pantry cards, standard recipe item cards, and action sheets.
- **Large elements (24px to 28px / `rounded-2xl`)**: Bottom sheets, meal category carousels, hero cooking mode cards, and dialog containers.
- **Full round (9999px / pill)**: Material 3 Assist/Filter Chips, Floating Action Buttons (Extended), and segmented meal switchers (Breakfast / Lunch / Dinner).

## Components

### Buttons
- **Filled Button**: Primary CTA (e.g., "Start Cooking", "Add to Pantry"). Background: `primary` (`#F08C65`), text/icon: `#491C08` (high-contrast deep warm brown). Rounded to 9999px (full pill). Height: 44px on mobile for generous touch accessibility.
- **Tonal Button**: Secondary actions (e.g., "Adjust Servings"). Background: `surface-container-high` (`#2C2927`), text: `primary` (`#F08C65`).
- **Outlined Button**: Border: 1px solid `outline` (`#524C48`), text: `on-surface` (`#ECE0DB`). Hover/active states fill with 8% `on-surface` overlay.

### Chips
- **Filter & Dietary Badges**: 32px height, 8px rounded corners (`rounded-md`) or full pill. Inactive state: `surface-container` (`#211F1D`) with `outline-variant` border. Active state: fills with `secondary` (`#82BDA8`) tinted at 20% opacity with `secondary` solid text (`#A1DEC9`).
- **Expiration / Stock Alert Chips**: Tertiary fill (`#E8BE6B` at 15% opacity) with `#F5D693` text for "Use within 2 days".

### Lists & Inventory Rows
- Single and multi-line pantry rows utilize 56px to 72px heights. Separated by clean dividers (`#2C2927`) or slight vertical gaps using `surface-container-low` cards. Leading visual contains an ingredient thumbnail (rounded-md, 40px) or icon container; trailing visual hosts stock counters, expiration labels, or swipe-to-delete actions.

### Checkboxes & Radio Buttons
- 20px target centered in 48px hit area. Unchecked: 2px border of `outline` (`#524C48`). Checked: filled with `primary` (`#F08C65`) with `surface` (`#141211`) checkmark glyph.

### Input Fields
- Filled variant with `surface-container-highest` (`#373432`) background, bottom active border in `primary` (`#F08C65`) (2px when focused), label floating with `on-surface-variant` (`#B5A8A2`), rounded top corners (8px). Caret color is `primary`.

### Cards
- **Recipe Card**: Built with `surface-container-low` (`#1D1B1A`) with `rounded-2xl` (16–24px) corners. Features full-bleed aspect-ratio-bound imagery on top, followed by 16px internal padding for title, cooking time, and dietary indicator chips.
- **Pantry Metric Card**: `surface-container` (`#211F1D`) housing rapid inventory totals, low-stock warnings, and barcode scanner launch buttons.

### Domain-Specific Components
- **Cooking Mode Step Sheet**: Full-screen or modal bottom sheet (`surface-container-high`) hosting extra-large step indices, integrated timer pills (`#82BDA8`), and high-contrast step descriptions.
- **Pantry Stock Meter**: A segmented micro-progress bar displaying freshness or volume, transitioning from `secondary` (Fresh) through `tertiary` (Expiring) to terracotta alert.