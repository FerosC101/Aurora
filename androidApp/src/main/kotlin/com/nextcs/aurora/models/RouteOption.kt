package com.nextcs.aurora.models

data class RouteOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: RouteType,
    val distance: Double, // km
    val duration: Int, // minutes
    val trafficLevel: TrafficLevel,
    val tollCost: Double = 0.0, // currency
    val fuelCost: Double = 0.0, // estimated
    val highlights: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val co2Saved: Double = 0.0 // kg compared to regular route
)

enum class RouteType {
    FASTEST,
    SHORTEST,
    ECO_FRIENDLY,
    AVOID_TOLLS,
    AVOID_HIGHWAYS,
    SCENIC
}

enum class TrafficLevel {
    LIGHT,
    MODERATE,
    HEAVY,
    SEVERE
}

object RouteGenerator {
    fun generateAlternatives(origin: String, destination: String): List<RouteOption> {
        // Generate 5 route options with different characteristics
        return listOf(
            RouteOption(
                name = "Fastest Route",
                type = RouteType.FASTEST,
                distance = 12.5,
                duration = 18,
                trafficLevel = TrafficLevel.MODERATE,
                tollCost = 45.0,
                fuelCost = 85.0,
                highlights = listOf("Via expressway", "Minimal stops"),
                warnings = listOf("Toll road")
            ),
            RouteOption(
                name = "Shortest Distance",
                type = RouteType.SHORTEST,
                distance = 10.2,
                duration = 25,
                trafficLevel = TrafficLevel.HEAVY,
                tollCost = 0.0,
                fuelCost = 70.0,
                highlights = listOf("Direct route", "No tolls"),
                warnings = listOf("Heavy traffic", "Multiple stoplights")
            ),
            RouteOption(
                name = "Eco-Friendly",
                type = RouteType.ECO_FRIENDLY,
                distance = 11.8,
                duration = 22,
                trafficLevel = TrafficLevel.LIGHT,
                tollCost = 20.0,
                fuelCost = 65.0,
                highlights = listOf("Smooth traffic flow", "Fewer stops"),
                warnings = emptyList(),
                co2Saved = 1.2
            ),
            RouteOption(
                name = "Avoid Tolls",
                type = RouteType.AVOID_TOLLS,
                distance = 14.5,
                duration = 28,
                trafficLevel = TrafficLevel.MODERATE,
                tollCost = 0.0,
                fuelCost = 95.0,
                highlights = listOf("No toll fees", "Scenic areas"),
                warnings = listOf("Longer duration")
            ),
            RouteOption(
                name = "Scenic Route",
                type = RouteType.SCENIC,
                distance = 16.2,
                duration = 32,
                trafficLevel = TrafficLevel.LIGHT,
                tollCost = 15.0,
                fuelCost = 100.0,
                highlights = listOf("Beautiful views", "Less congested", "Parks nearby"),
                warnings = listOf("Longer trip")
            )
        )
    }
}
