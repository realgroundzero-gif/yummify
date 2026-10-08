package de.yummify.app.data.local

import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.Recipe

/** Demo recipes shown until a Notion recipe database is connected. */
object SampleData {

    val recipes = listOf(
        Recipe(
            id = "1",
            title = "Cremige Tomaten-Burrata-Pasta",
            description = "Mit sonnengereiften San-Marzano-Tomaten, Knoblauchöl und frischem Basilikum in nur 20 Minuten auf dem Tisch.",
            imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800&q=80",
            cookTimeMinutes = 20,
            calories = 560,
            proteinGrams = 22,
            carbsGrams = 68,
            fatGrams = 24,
            category = "pasta",
            tags = listOf("Vegetarisch", "Pasta", "Wochenfavorit"),
            score = 4.9,
            isFavorite = true,
            isHeroOfDay = true,
            difficulty = "Einfach",
            estimatedCost = "€€ (Günstig)",
            lastCookedDate = "Vor 2 Tagen",
            defaultServings = 2,
            ingredients = listOf(
                Ingredient("Rigatoni oder Penne", 250.0, "g", "Vorrat", true),
                Ingredient("Frische Burrata", 1.0, "Kugel", "Kühlregal", false),
                Ingredient("Kirschtomaten", 200.0, "g", "Obst & Gemüse", false),
                Ingredient("Knoblauch", 3.0, "Zehen", "Vorrat", true),
                Ingredient("Basilikum frisch", 1.0, "Bund", "Obst & Gemüse", false),
                Ingredient("Olivenöl extra vergine", 3.0, "EL", "Vorrat", true),
                Ingredient("Parmesan gerieben", 50.0, "g", "Kühlregal", true),
                Ingredient("Salz & Pfeffer", 1.0, "", "Gewürze", true),
                Ingredient("Chiliflocken", 1.0, "TL", "Gewürze", true)
            ),
            instructions = listOf(
                "Pasta in reichlich Salzwasser al dente kochen. 1 Tasse Nudelwasser aufheben.",
                "Kirschtomaten waschen und halbieren. Knoblauch fein hacken.",
                "Olivenöl in einer großen Pfanne erhitzen, Knoblauch und Chiliflocken 1 Min. anbraten.",
                "Tomaten hinzufügen, mit Salz würzen. 8 Min. köcheln lassen bis sie aufplatzen.",
                "Gekochte Pasta und etwas Nudelwasser in die Sauce geben, gut vermengen.",
                "Auf Tellern anrichten, Burrata in der Mitte platzieren.",
                "Mit frischem Basilikum, Parmesan und Olivenöl beenden. Sofort servieren."
            )
        ),
        Recipe(
            id = "2",
            title = "Mediterrane Lachs-Bowl",
            description = "Knusprig gebratener Lachs mit Quinoa, Kirschtomaten, Avocado und cremigem Tzatziki.",
            imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
            cookTimeMinutes = 25,
            calories = 620,
            proteinGrams = 45,
            carbsGrams = 38,
            fatGrams = 28,
            category = "protein",
            tags = listOf("High Protein", "Omega-3", "Glutenfrei"),
            score = 4.8,
            isFavorite = false,
            lastCookedDate = "Gestern",
            defaultServings = 2,
            ingredients = listOf(
                Ingredient("Lachsfilet", 300.0, "g", "Fisch & Fleisch", false),
                Ingredient("Quinoa", 160.0, "g", "Vorrat", true),
                Ingredient("Kirschtomaten", 150.0, "g", "Obst & Gemüse", false),
                Ingredient("Salatgurke", 1.0, "", "Obst & Gemüse", false),
                Ingredient("Kalamata Oliven", 60.0, "g", "Vorrat", true),
                Ingredient("Feta-Käse", 80.0, "g", "Kühlregal", false),
                Ingredient("Griechischer Joghurt", 150.0, "g", "Kühlregal", false),
                Ingredient("Zitronensaft", 2.0, "EL", "Obst & Gemüse", false),
                Ingredient("Dill frisch", 0.5, "Bund", "Obst & Gemüse", false),
                Ingredient("Olivenöl", 2.0, "EL", "Vorrat", true),
                Ingredient("Salz, Pfeffer, Paprikapulver", 1.0, "", "Gewürze", true)
            ),
            instructions = listOf(
                "Quinoa in doppelter Menge gesalzenem Wasser 15 Min. kochen. Ruhen lassen.",
                "Joghurt mit Gurfe, Knoblauch, Dill, Zitronensaft zu Tzatziki mixen.",
                "Lachs mit Paprikapulver, Salz und Pfeffer würzen.",
                "Olivenöl in Pfanne erhitzen, Lachs 3-4 Min. pro Seite goldbraun braten.",
                "Bowl aufbauen: Quinoa als Base, Lachs obenauf.",
                "Gemüse, Oliven und Feta verteilen, Tzatziki danebengeben.",
                "Mit Zitronenspalt und frischem Dill garnieren."
            )
        ),
        Recipe(
            id = "3",
            title = "Kichererbsen-Curry mit Kokosmilch",
            description = "Wärmendes One-Pot-Curry mit knackigen Kichererbsen, Spinat und cremiger Kokosmilch.",
            imageUrl = "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800&q=80",
            cookTimeMinutes = 30,
            calories = 480,
            proteinGrams = 18,
            carbsGrams = 55,
            fatGrams = 22,
            category = "onepot",
            tags = listOf("Vegan", "Glutenfrei", "One-Pot"),
            score = 4.7,
            isFavorite = true,
            lastCookedDate = "Vor 3 Tagen",
            defaultServings = 3,
            ingredients = listOf(
                Ingredient("Kichererbsen (Dose)", 480.0, "g", "Vorrat", true),
                Ingredient("Kokosmilch", 400.0, "ml", "Vorrat", true),
                Ingredient("Passierte Tomaten", 250.0, "ml", "Vorrat", true),
                Ingredient("Blattspinat (TK)", 200.0, "g", "Tiefkühl", true),
                Ingredient("Zwiebeln", 2.0, "", "Obst & Gemüse", false),
                Ingredient("Knoblauch", 4.0, "Zehen", "Vorrat", true),
                Ingredient("Ingwer frisch", 2.0, "cm", "Obst & Gemüse", false),
                Ingredient("Currypulver", 2.0, "TL", "Gewürze", true),
                Ingredient("Garam Masala", 1.0, "TL", "Gewürze", true),
                Ingredient("Kurkuma", 0.5, "TL", "Gewürze", true),
                Ingredient("Basmatireis", 200.0, "g", "Vorrat", true)
            ),
            instructions = listOf(
                "Zwiebeln, Knoblauch und Ingwer fein würfeln.",
                "Öl in einem großen Topf erhitzen, Zwiebeln 5 Min. glasig dünsten.",
                "Knoblauch, Ingwer und alle Gewürze 2 Min. mitbraten.",
                "Tomaten, Kokosmilch und Kichererbsen hinzufügen. Aufkochen.",
                "20 Min. bei mittlerer Hitze köcheln lassen.",
                "Tiefkühl-Spinat einrühren, 3 Min. mitköcheln.",
                "Mit Salz abschmecken, auf Basmatireis servieren."
            )
        ),
        Recipe(
            id = "4",
            title = "Smashed Avocado & Egg Sourdough",
            description = "Knuspriges Sauerteigbrot mit cremiger Avocado, weichem Spiegelei und Chiliflocken.",
            imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&q=80",
            cookTimeMinutes = 15,
            calories = 390,
            proteinGrams = 18,
            carbsGrams = 32,
            fatGrams = 22,
            category = "quick",
            tags = listOf("Schnell", "Vegetarisch", "Frühstück"),
            score = 4.6,
            isFavorite = false,
            lastCookedDate = "Heute Morgen",
            defaultServings = 1,
            ingredients = listOf(
                Ingredient("Sauerteigbrot", 2.0, "Scheiben", "Bäckerei", false),
                Ingredient("Avocado (reif)", 1.0, "", "Obst & Gemüse", false),
                Ingredient("Eier", 2.0, "", "Kühlregal", true),
                Ingredient("Zitronensaft", 1.0, "TL", "Obst & Gemüse", false),
                Ingredient("Chiliflocken", 0.5, "TL", "Gewürze", true),
                Ingredient("Microgreens oder Kresse", 1.0, "Handvoll", "Obst & Gemüse", false),
                Ingredient("Olivenöl", 1.0, "EL", "Vorrat", true),
                Ingredient("Salz, Pfeffer", 1.0, "", "Gewürze", true)
            ),
            instructions = listOf(
                "Brot im Toaster oder Pfanne goldbraun toasten.",
                "Avocado halbieren, entkernen und in eine Schüssel geben.",
                "Mit Zitronensaft, Salz und Pfeffer grob zerdrücken.",
                "Eier in etwas Öl braten – nach Belieben Spiegel- oder Rührei.",
                "Avocadomasse dick auf die getoasteten Scheiben streichen.",
                "Ei obenauf legen, mit Chiliflocken und Microgreens garnieren.",
                "Sofort servieren."
            )
        ),
        Recipe(
            id = "5",
            title = "Orangen-Walnuss-Kuchen",
            description = "Saftiger Kuchen mit frischem Orangenaroma, gehackten Walnüssen und zarter Zuckerglasur.",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=800&q=80",
            cookTimeMinutes = 55,
            calories = 380,
            proteinGrams = 7,
            carbsGrams = 48,
            fatGrams = 19,
            category = "baking",
            tags = listOf("Backen", "Vegetarisch", "Süß"),
            score = 4.8,
            isFavorite = true,
            lastCookedDate = "Vor 1 Woche",
            defaultServings = 12,
            ingredients = listOf(
                Ingredient("Mehl", 250.0, "g", "Vorrat", true),
                Ingredient("Zucker", 180.0, "g", "Vorrat", true),
                Ingredient("Backpulver", 1.5, "TL", "Vorrat", true),
                Ingredient("Eier", 3.0, "", "Kühlregal", true),
                Ingredient("Butter weich", 120.0, "g", "Kühlregal", false),
                Ingredient("Orangensaft frisch", 100.0, "ml", "Obst & Gemüse", false),
                Ingredient("Orangenzeste", 2.0, "EL", "Obst & Gemüse", false),
                Ingredient("Walnüsse gehackt", 100.0, "g", "Vorrat", false),
                Ingredient("Puderzucker", 80.0, "g", "Vorrat", true)
            ),
            instructions = listOf(
                "Ofen auf 175°C vorheizen. Kastenform einfetten.",
                "Butter und Zucker cremig schlagen. Eier einzeln unterrühren.",
                "Orangensaft und Orangenzeste einarbeiten.",
                "Mehl und Backpulver sieben, langsam unter die Masse heben.",
                "Walnüsse unterrühren.",
                "Teig in Kastenform füllen. 50-55 Min. backen.",
                "Stäbchenprobe machen. Auskühlen lassen.",
                "Puderzucker mit etwas Orangensaft zu Glasur anrühren und verteilen."
            )
        ),
        Recipe(
            id = "6",
            title = "Zucchini-Carbonara",
            description = "Die klassische Carbonara neu gedacht – mit geriebener Zucchini für extra Cremigkeit und Leichtigkeit.",
            imageUrl = "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=800&q=80",
            cookTimeMinutes = 22,
            calories = 520,
            proteinGrams = 25,
            carbsGrams = 62,
            fatGrams = 18,
            category = "pasta",
            tags = listOf("Pasta", "Schnell", "Vegetarisch"),
            score = 4.7,
            isFavorite = false,
            lastCookedDate = "Vor 5 Tagen",
            defaultServings = 2,
            ingredients = listOf(
                Ingredient("Spaghetti", 200.0, "g", "Vorrat", true),
                Ingredient("Zucchini", 2.0, "", "Obst & Gemüse", false),
                Ingredient("Eier", 3.0, "", "Kühlregal", true),
                Ingredient("Parmesan", 80.0, "g", "Kühlregal", true),
                Ingredient("Pecorino", 30.0, "g", "Kühlregal", false),
                Ingredient("Knoblauch", 2.0, "Zehen", "Vorrat", true),
                Ingredient("Olivenöl", 2.0, "EL", "Vorrat", true),
                Ingredient("Schwarzer Pfeffer grob", 1.0, "TL", "Gewürze", true)
            ),
            instructions = listOf(
                "Pasta in Salzwasser kochen. Nudelwasser aufheben.",
                "Zucchini grob reiben, Flüssigkeit leicht ausdrücken.",
                "Eier mit Parmesan und Pecorino verquirlen.",
                "Knoblauch in Öl anbraten. Geriebene Zucchini dazugeben, 3 Min. anschwitzen.",
                "Hitze ausschalten. Pasta dazugeben, Ei-Käse-Mischung einrühren.",
                "Mit Nudelwasser cremig rühren – Hitze darf die Eier nicht stocken lassen!",
                "Großzügig mit Pfeffer würzen und sofort servieren."
            )
        )
    )
}
