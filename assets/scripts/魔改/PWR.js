const library = require("魔改/base/PWR-library");

const model = {
    MinPressure: 15,
    MaxPressure: 25,
    MaxFuelRod: 60,
    MaxCoreCoolant: 200,
    BoilingTemp: 600,
    SecCoolant: "water",
    SteamOutput: "索拉里斯2.0-蒸汽",
    BoilingPoint: 100,
    OutputCoefficient: 0.5,
    Parameter: [4200, 0.8, 24, 5, 8000],
    needPower: true,
};

const fuelrods = [
    {
        FuelRodSupport: "thorium",
        SpentFuel: "scrap",
        RadiationFunction: "[green]SAFE/SQUARE ROOT",
        Functions: [2, 15, 5],
        HurtperFlux: 0.03,
        Durable: 0.015,
        Calorific: 1,
    },
    {
        FuelRodSupport: "phase-fabric",
        SpentFuel: "silicon",
        RadiationFunction: "[green]SAFE/CONSTANT",
        Functions: [0, 160],
        HurtperFlux: 0.3,
        Durable: 1.5,
        Calorific: 1.5,
    },
    {
        FuelRodSupport: "surge-alloy",
        SpentFuel: "metaglass",
        RadiationFunction: "[red]DANGEROUS/LINEAR",
        Functions: [3, 0.02, 0.5],
        HurtperFlux: 4,
        Durable: 0.6,
        Calorific: 2,
    },
];

const def = {
    localizedName: "PWR反应堆",
    health: 5400,
    size: 6,
    hasPower: true,
    hasLiquids: true,
    hasItems: true,
    itemCapacity: 20,
    liquidCapacity: 800,
    canOverdrive: false,
    squareSprite: true,
    category: Category.power,
    drawer: extend(DrawMulti, {
        drawers: [
            extend(DrawRegion, {suffix: "-bottom"}),
            new DrawDefault(),
            extend(DrawLiquidRegion, {drawLiquid: Liquids.water, suffix: "-liquid"}),
        ],
    }),
};

const pwr = library.PWRv2(GenericCrafter, GenericCrafter.GenericCrafterBuild, "PWR", model, fuelrods, def);
pwr.setupRequirements(Category.power, ItemStack.with());
