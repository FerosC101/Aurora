package org.aurora.traffic.model

import kotlin.random.Random

class CityMap {
    val intersections: MutableMap<String, Intersection> = mutableMapOf()
    val roads: MutableMap<String, Road> = mutableMapOf()
    val vehicles: MutableMap<String, Vehicle> = mutableMapOf()
    
    init {
        createDefaultCity()
    }
    
    private fun createDefaultCity() {
        // Create a 3x2 grid of intersections (6 total)
        val gridSpacing = 300f // meters between intersections
        
        // Create intersections
        for (row in 0..1) {
            for (col in 0..2) {
                val id = "I${row}${col}"
                val x = col * gridSpacing + 100f
                val y = row * gridSpacing + 100f
                
                intersections[id] = Intersection(
                    id = id,
                    position = Position(x, y),
                    isSignalized = true
                )
            }
        }
        
        // Create horizontal roads (East-West)
        for (row in 0..1) {
            for (col in 0..1) {
                val startId = "I${row}${col}"
                val endId = "I${row}${col + 1}"
                val start = intersections[startId]!!
                val end = intersections[endId]!!
                
                // Eastbound road
                val roadEastId = "${startId}_${endId}_E"
                roads[roadEastId] = Road(
                    id = roadEastId,
                    startIntersection = startId,
                    endIntersection = endId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.EAST,
                    positions = listOf(start.position, end.position)
                )
                
                // Westbound road
                val roadWestId = "${endId}_${startId}_W"
                roads[roadWestId] = Road(
                    id = roadWestId,
                    startIntersection = endId,
                    endIntersection = startId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.WEST,
                    positions = listOf(end.position, start.position)
                )
                
                intersections[startId]!!.connectedRoads.add(roadEastId)
                intersections[endId]!!.connectedRoads.add(roadWestId)
            }
        }
        
        // Create vertical roads (North-South)
        for (col in 0..2) {
            for (row in 0..0) {
                val startId = "I${row}${col}"
                val endId = "I${row + 1}${col}"
                val start = intersections[startId]!!
                val end = intersections[endId]!!
                
                // Southbound road
                val roadSouthId = "${startId}_${endId}_S"
                roads[roadSouthId] = Road(
                    id = roadSouthId,
                    startIntersection = startId,
                    endIntersection = endId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.SOUTH,
                    positions = listOf(start.position, end.position)
                )
                
                // Northbound road
                val roadNorthId = "${endId}_${startId}_N"
                roads[roadNorthId] = Road(
                    id = roadNorthId,
                    startIntersection = endId,
                    endIntersection = startId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.NORTH,
                    positions = listOf(end.position, start.position)
                )
                
                intersections[startId]!!.connectedRoads.add(roadSouthId)
                intersections[endId]!!.connectedRoads.add(roadNorthId)
            }
        }
        
        // Setup traffic lights
        setupTrafficLights()
    }
    
    private fun setupTrafficLights() {
        intersections.values.forEach { intersection ->
            // Create lights for each direction
            Direction.values().forEach { direction ->
                val light = TrafficLight(
                    id = "${intersection.id}_${direction}",
                    intersectionId = intersection.id,
                    direction = direction,
                    state = if (direction == Direction.EAST || direction == Direction.WEST) 
                        LightState.GREEN else LightState.RED,
                    remainingTime = 15f
                )
                intersection.trafficLights[direction] = light
            }
        }
    }
    
    fun spawnVehicles(count: Int) {
        val intersectionIds = intersections.keys.toList()
        
        repeat(count) { i ->
            val startIntersection = intersectionIds.random()
            var endIntersection = intersectionIds.random()
            
            // Ensure different start and end
            while (endIntersection == startIntersection) {
                endIntersection = intersectionIds.random()
            }
            
            val route = findShortestPath(startIntersection, endIntersection)
            if (route.isNotEmpty()) {
                val startPos = intersections[startIntersection]!!.position
                val vehicle = Vehicle(
                    id = "V$i",
                    position = startPos.copy(),
                    destination = endIntersection,
                    route = route,
                    maxSpeed = Random.nextFloat() * 20f + 40f,
                    color = VehicleColor.values().random()
                )
                vehicles[vehicle.id] = vehicle
            }
        }
    }
    
    fun findShortestPath(start: String, end: String): List<String> {
        // Simple BFS pathfinding
        val queue = mutableListOf(listOf(start))
        val visited = mutableSetOf(start)
        val pathToRoad = mutableMapOf<String, List<String>>()
        
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.last()
            
            if (current == end) {
                // Convert intersection path to road path
                val roadPath = mutableListOf<String>()
                for (i in 0 until path.size - 1) {
                    val from = path[i]
                    val to = path[i + 1]
                    val road = roads.values.find { 
                        it.startIntersection == from && it.endIntersection == to 
                    }
                    road?.let { roadPath.add(it.id) }
                }
                return roadPath
            }
            
            val intersection = intersections[current] ?: continue
            
            // Find connected intersections
            intersection.connectedRoads.forEach { roadId ->
                val road = roads[roadId]
                if (road != null && road.startIntersection == current) {
                    val next = road.endIntersection
                    if (next !in visited) {
                        visited.add(next)
                        queue.add(path + next)
                    }
                }
            }
        }
        
        return emptyList() // No path found
    }
    
    fun updateCongestionLevels() {
        roads.values.forEach { road ->
            val vehiclesOnRoad = vehicles.values.count { it.currentRoad == road.id }
            road.updateCongestionLevel(vehiclesOnRoad)
        }
    }
    
    fun getVehiclesNear(position: Position, radius: Float): List<Vehicle> {
        return vehicles.values.filter { 
            !it.hasReachedDestination && it.position.distanceTo(position) < radius 
        }
    }
}
