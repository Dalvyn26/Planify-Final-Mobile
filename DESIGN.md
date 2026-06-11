---
name: Minimalist 8-Bit Productivity
colors:
  surface: '#fcf9f8'
  surface-dim: '#dcd9d9'
  surface-bright: '#fcf9f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3f2'
  surface-container: '#f0eded'
  surface-container-high: '#eae7e7'
  surface-container-highest: '#e5e2e1'
  on-surface: '#1c1b1b'
  on-surface-variant: '#444748'
  inverse-surface: '#313030'
  inverse-on-surface: '#f3f0ef'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1c1b1b'
  on-primary-container: '#858383'
  inverse-primary: '#c8c6c5'
  secondary: '#5e604d'
  on-secondary: '#ffffff'
  secondary-container: '#e1e1c9'
  on-secondary-container: '#636451'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1c1b1a'
  on-tertiary-container: '#868382'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474746'
  secondary-fixed: '#e4e4cc'
  secondary-fixed-dim: '#c8c8b0'
  on-secondary-fixed: '#1b1d0e'
  on-secondary-fixed-variant: '#474836'
  tertiary-fixed: '#e6e2df'
  tertiary-fixed-dim: '#cac6c4'
  on-tertiary-fixed: '#1c1b1a'
  on-tertiary-fixed-variant: '#484645'
  background: '#fcf9f8'
  on-background: '#1c1b1b'
  surface-variant: '#e5e2e1'
typography:
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
    letterSpacing: 0em
  body-md:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.1em
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin: 24px
---

## Brand & Style

This design system establishes a high-contrast, binary visual language that bridges the gap between 1980s computing and contemporary productivity workflows. The brand personality is disciplined, nostalgic, and hyper-functional. It targets "power-minimalists"—users who find clarity in constraints and beauty in low-fidelity aesthetics.

The design style is a synthesis of **Retro-Digital** and **Brutalism**. It avoids all modern flourishes like gradients, blurs, or rounded corners in favor of a rigid, grid-locked interface. The emotional response is one of "focused play"—the efficiency of a modern SaaS tool delivered through the charming, tactile lens of an 8-bit game console.

## Colors

The palette is strictly limited to two core tones to maximize focus and maintain the monochrome 8-bit aesthetic. 

- **Fade Black (#1A1A1A):** Used for all structural elements including borders, text, icons, and solid fills for active states. It provides a softer contrast than pure black, mimicking aged CRT phosphor or high-quality ink.
- **Light Cream (#F5F5DC):** The primary canvas color. It provides a vintage, paper-like warmth that reduces eye strain compared to stark white.

UI states are handled via inversion: if an element is Light Cream with a Fade Black border, its active or hovered state becomes solid Fade Black with Light Cream text. No grays or mid-tones are permitted.

## Typography

This design system uses **Space Grotesk** across all levels. While not a literal pixel font, its geometric construction and technical "ink traps" evoke a digital, programmed feel that remains highly readable for productivity tasks.

- **Headlines:** Set in bold weights with tight tracking to mimic the blocky impact of old title screens.
- **Labels:** Always uppercase with increased letter spacing to serve as clear UI anchors and navigation points.
- **Body:** Standard weight with generous line heights to ensure the "minimalist" aspect of the system balances the "retro" density.

For a true 8-bit feel, avoid any font smoothing or anti-aliasing in CSS where possible to keep character edges sharp.

## Layout & Spacing

The layout is governed by a **strict 4px baseline grid**. Every element, padding value, and margin must be a multiple of 4. 

The system utilizes a **Fixed Grid** approach for desktop (12 columns) and a fluid single-column approach for mobile. 
- **Borders as Spacing:** In this design system, borders are 2px wide and sit inside the grid, not outside. 
- **Negative Space:** Use "blocky" whitespace. Instead of organic margins, use significant jumps (e.g., from 16px to 32px) to create clear, stepped sections reminiscent of tiled game maps.

## Elevation & Depth

Depth is conveyed through **Bold Borders** and **Offset Fills**, rather than shadows or blurs. 

- **Level 0 (Surface):** Light Cream background.
- **Level 1 (Planes):** Defined by a 2px Fade Black border. 
- **Level 2 (Interaction):** A "Hard Shadow" effect is achieved by placing a solid Fade Black rectangle behind a component, offset by 4px or 8px to the bottom-right.
- **Active State:** Elements do not "lift"; they invert. A button or card "pressed" state is indicated by swapping the background and foreground colors.

## Shapes

The shape language is strictly **Rectilinear**. 
- All corners are sharp (0px radius).
- Lines and borders are always 2px or 4px thick; odd-numbered pixel widths are prohibited as they break the 8-bit "pixel-perfect" illusion.
- Icons should be designed on a 16x16 or 24x24 pixel grid, using only straight lines and 45-degree angles.

## Components

- **Buttons:** Large, rectangular blocks with a 2px border. Primary buttons use a "Hard Shadow" (offset fill). On hover, the shadow disappears and the button "moves" 2px down and right to simulate a physical press.
- **Inputs:** Simple boxes with a 2px border. The focus state is indicated by a thicker 4px border or a flashing "block" cursor (rectangular underscore).
- **Cards:** Used for grouping tasks or notes. They feature a header bar separated by a 2px horizontal line. Headers are always inverted (Fade Black background with Light Cream text).
- **Checkboxes:** Square 16px boxes. When checked, they are filled with a solid Fade Black square or an "X" mark made of 2px diagonal lines.
- **Lists:** Items are separated by 2px horizontal rules. Selection is indicated by a "pointer" icon (a small right-facing triangle/arrow) appearing to the left of the text.
- **Progress Bars:** A segmented bar where each "pixel block" represents 10% progress, rather than a smooth continuous fill.
- **Chips/Tags:** Small rectangular boxes with uppercase Label-sm text, used sparingly for categorization.