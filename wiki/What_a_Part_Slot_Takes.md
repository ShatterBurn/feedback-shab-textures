# What a Part Slot Takes

Every material slot of a modular part accepts stock by its **shape** and by its **properties**, not by a fixed list of items. A slot that takes a shape and a property takes any material that has both.

## Shapes

A stock item belongs to one shape class. A slot names the shapes it takes.

| Shape | Items |
| --- | --- |
| long piece | stick, rod, bone, bamboo stalk |
| sheet | plate, pane, foil, paper |
| hide | leather, hide |
| mat | carpets |
| gem | gem, shard |
| cord | string, wire, chain, fibre, bundle |
| lump | ingot, nugget, block, cobble, brick, slug |
| board | any planks, bamboo and nether wood included |
| loose fill | dust, raw plant matter, feathers, flint |

## Properties

Properties are named bands. A band has the same meaning on every slot.

| Property | Bands, lowest to highest |
| --- | --- |
| hardness | yielding, soft, firm, hard, adamant |
| heat conduction | insulator, poor, fair, good, excellent |
| strength | weak, fair, strong, very strong, extreme |
| weight | light, medium, heavy, very heavy |
| heat limit | low, moderate, high, extreme |
| toughness | brittle, tough, very tough |
| electric conduction | poor, good, excellent |

A slot asks for a band, or for a band or better ("at least fair strength"), or for a band or lower ("at most medium weight").

## Uses

A use is a named bundle of properties that slots ask for.

| Use | A material has it when |
| --- | --- |
| cushioning | it is yielding, light or medium in weight and a heat insulator; not food, explosive, corrosive or a fluid |
| fabric | it is yielding, tough and light or medium in weight; not food, explosive, corrosive or a fluid |
| stuffing | it is cushioning, or a fibrous heat-insulating loose matter |
| insulating | it is a heat insulator; not food, explosive, corrosive or a fluid |
| cordage | it is tough; not food, explosive, corrosive or a fluid |
| inert fill | it is medium weight or heavier; not food, explosive, corrosive or a fluid |

Soft metals such as tin and lead are not cushioning: they are heavy and conduct heat. A [bed](Bed.md) pad takes moss, wool, down, grass and leather, and does not take planks.

## Examples

- Handles need to be tough and at least fair in strength.
- Bamboo and nether wood planks fit every board slot.
- A [heat exchanger](Heat_Exchanger.md) fin, a regen jacket, an intercooler and a [dynamotor](Dynamotor.md) cooling plate need good heat conduction.
- A [battery](Battery.md) bay plate and a battery fitting plate on a [backpack](Backpack.md#fittings) need good electric conduction.
- An [electromagnet](Electromagnet.md) yoke must be magnetic.
- A burner flame holder needs a heat limit of moderate or better.
- A lamp ballast needs medium weight or heavier.
- The thermal and coil parts of a suit module need good heat or electric conduction ([Exosuit](Exosuit.md#upgrade-modules)).
- A gauge lens must be transparent.

## Reading a slot

On the [Blueprint Table](Blueprint_Table.md), hover a part's role in the diagram. The tooltip prints **Takes:** with the shapes the slot accepts and **Needs:** with the properties it asks for. Hold **Shift** to read the thresholds behind each band. A material that fails a slot says so on its swatch card and is not listed for that slot.

See [Composed Parts](Composed_Parts.md) for slots you choose and [Materials](Materials.md) for each material's figures.
