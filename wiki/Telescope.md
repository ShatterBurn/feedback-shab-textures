# Telescope

A hand-held telescope built from three parts: an objective lens, an eyepiece lens and a tube. Hold right click to look through it and see far things larger.

## Crafting

Shape the three parts at a part bench (see [Composed Parts](Composed_Parts.md)), then put them side by side in a crafting grid.

```
 Objective Lens | Eyepiece Lens | Telescope Tube
```

| Part | Axes | Stock |
| --- | --- | --- |
| Objective Lens | focal length 100 to 600 mm, lens width 20 to 80 mm | lens or gem stock |
| Eyepiece Lens | focal length 10 to 60 mm | lens or gem stock |
| Telescope Tube | length 110 to 660 mm | metal pipe or plate |

The lenses must be of a material that bends light: glass, quartz, amber or a gem (the materials with a **refractive index** on [Materials](Materials.md)). See [What a Part Slot Takes](What_a_Part_Slot_Takes.md) for what each slot accepts.

## Using it

Hold right click to look through it. The view narrows by the **magnification** inside a round scope mask, and a pale haze over the view shows the **blur**. Release to lower it. Looking does not wear it. The tooltip shows magnification, blur and mass.

## How it works

- **Magnification** is the objective's focal length divided by the eyepiece's, up to 20x. A long objective with a short eyepiece magnifies most.
- **Blur** has two causes. A tube whose length is not the sum of the two focal lengths puts the lenses out of focus; the further off, the more haze. A wide objective of glass that bends light weakly blurs too: the weakness is the lens width over its focal length, over the refractive index less one. A longer focal length, a narrower lens or a material with a higher index lowers it.
- The tube's metal sets the mass.

Because the tube length is its own axis, match it to the lenses: for a 300 mm objective and a 30 mm eyepiece, a 330 mm tube.

See also [Optics](Optics.md) for lenses and the pyrometer.
