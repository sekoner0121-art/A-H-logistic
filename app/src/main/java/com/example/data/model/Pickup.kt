package com.example.data.model

enum class PickupStatus(val label: String, val stepIndex: Int) {
    PENDIENTE("Nueva Recolección", 0),
    ACEPTADA("Aceptada en Espera", 1),
    EN_CAMINO("En Camino al Sitio", 2),
    EN_SITIO("En Sitio / Cargando", 3),
    RECOLECTADA("Recolectada / Exitosa", 4),
    INCIDENCIA("Con Incidencia", -1),
    RECHAZADA("Rechazada", -1)
}

enum class Priority(val label: String) {
    NORMAL("Normal"),
    ALTA("Alta Prioridad"),
    URGENTE("Urgente")
}

enum class DriverStatus(val label: String) {
    DISPONIBLE("En Turno / Disponible"),
    EN_RUTA("En Ruta / Ocupado"),
    FUERA_TURNO("Fuera de Turno / Descanso")
}

data class Pickup(
    val id: String = "",
    val trackingCode: String = "",
    val clientName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val address: String = "",
    val reference: String = "",
    val timeWindow: String = "",
    val packagesCount: Int = 1,
    val totalWeightKg: Double = 0.0,
    val packageType: String = "Cajas estándar",
    val priority: Priority = Priority.NORMAL,
    val status: PickupStatus = PickupStatus.PENDIENTE,
    val notes: String = "",
    val specialInstructions: String = "",
    val signatureName: String = "",
    val hasSignature: Boolean = false,
    val evidenceNotes: String = "",
    val incidentReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val driverId: String = "",
    val unitId: String = "",
    val torreId: String = "TORRE-CENTRAL-01",
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332
)

data class DriverProfile(
    val id: String = "DRV-8821",
    val name: String = "Carlos Mendoza",
    val email: String = "sekoner0121@gmail.com",
    val phone: String = "+52 55 9876 5432",
    val unitId: String = "UNIDAD-504",
    val licensePlate: String = "LC-884-TX",
    val vehicleType: String = "Camioneta 3.5T Cerrada",
    val status: DriverStatus = DriverStatus.DISPONIBLE,
    val currentTorreId: String = "TORRE-CENTRAL-01",
    val lastLatitude: Double = 19.4326,
    val lastLongitude: Double = -99.1332,
    val batteryPct: Int = 92
)

data class TowerMessage(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isUrgent: Boolean = false
)

data class TowerConnectionState(
    val isLinked: Boolean = true,
    val torreId: String = "TORRE-CENTRAL-01",
    val serverStatus: String = "Conectado en Vivo",
    val channel: String = "firestore://logistica/torres/TORRE-CENTRAL-01",
    val lastPingTime: Long = System.currentTimeMillis(),
    val totalDispatchedToday: Int = 14,
    val activeDriversInTower: Int = 8
)
