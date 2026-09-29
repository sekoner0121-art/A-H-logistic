package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirebaseManager
import com.example.data.model.DriverProfile
import com.example.data.model.DriverStatus
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.example.data.model.Priority
import com.example.data.model.TowerConnectionState
import com.example.data.model.TowerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class LogisticsRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val firebaseManager = FirebaseManager(context)

    private val _driverProfile = MutableStateFlow(
        DriverProfile(
            id = "DRV-9942",
            name = "Conductor 1",
            email = "conductor1@example.invalid",
            phone = "+52 55 4912 3340",
            unitId = "UNIDAD-504",
            licensePlate = "LC-884-TX",
            vehicleType = "Camioneta 3.5T Cerrada",
            status = DriverStatus.DISPONIBLE,
            currentTorreId = "TORRE-CENTRAL-01",
            lastLatitude = 19.4326,
            lastLongitude = -99.1332,
            batteryPct = 89
        )
    )
    val driverProfile: StateFlow<DriverProfile> = _driverProfile.asStateFlow()

    private val _towerConnection = MutableStateFlow(
        TowerConnectionState(
            isLinked = true,
            torreId = "TORRE-CENTRAL-01",
            serverStatus = "Enlazado en Vivo",
            channel = "firestore://torres/TORRE-CENTRAL-01/recolecciones",
            lastPingTime = System.currentTimeMillis(),
            totalDispatchedToday = 18,
            activeDriversInTower = 9
        )
    )
    val towerConnection: StateFlow<TowerConnectionState> = _towerConnection.asStateFlow()

    private val _pickups = MutableStateFlow<List<Pickup>>(getInitialPickups())
    val pickups: StateFlow<List<Pickup>> = _pickups.asStateFlow()

    private val _incomingPickupAlert = MutableStateFlow<Pickup?>(null)
    val incomingPickupAlert: StateFlow<Pickup?> = _incomingPickupAlert.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _towerMessages = MutableStateFlow<List<TowerMessage>>(getInitialTowerMessages())
    val towerMessages: StateFlow<List<TowerMessage>> = _towerMessages.asStateFlow()

    init {
        // Sync with Firestore if available
        scope.launch {
            firebaseManager.saveDriverPing(_driverProfile.value)
        }
    }

    private fun getInitialPickups(): List<Pickup> {
        val now = System.currentTimeMillis()
        return listOf(
            Pickup(
                id = "REC-801",
                trackingCode = "REC-801-CDMX",
                clientName = "Distribuidora Médica del Valle",
                contactPerson = "Lic. Roberto Garza",
                phone = "+52 55 5678 1234",
                address = "Av. Insurgentes Sur 1450, Col. Actipan, Benito Juárez, CDMX",
                reference = "Andén 3 posterior, acceso proveedores por calle Santander",
                timeWindow = "09:30 - 10:30 hrs",
                packagesCount = 4,
                totalWeightKg = 32.5,
                packageType = "Insumos médicos refrigerados",
                priority = Priority.URGENTE,
                status = PickupStatus.EN_CAMINO,
                notes = "Requiere sello de cadena de frío y revisión de temperatura en recepción.",
                specialInstructions = "Pedir acceso con guardia 'Puerta B'. Preguntar por Ing. Morales.",
                driverId = "DRV-9942",
                unitId = "UNIDAD-504",
                torreId = "TORRE-CENTRAL-01",
                createdAt = now - 3600000,
                updatedAt = now - 1200000
            ),
            Pickup(
                id = "REC-802",
                trackingCode = "REC-802-TLP",
                clientName = "Manufacturas Industriales Azteca",
                contactPerson = "Ing. Sofía Carvajal",
                phone = "+52 55 4123 9876",
                address = "Calle Industria Textil 45, Parque Industrial Tlalnepantla",
                reference = "Bodega 8-A, portón azul grande",
                timeWindow = "11:00 - 12:30 hrs",
                packagesCount = 12,
                totalWeightKg = 145.0,
                packageType = "Cajas de autopartes y pernos",
                priority = Priority.ALTA,
                status = PickupStatus.ACEPTADA,
                notes = "Cargar con patín hidráulico disponible en rampa.",
                specialInstructions = "Llevar chaleco reflejante y botas de seguridad obligatorios.",
                driverId = "DRV-9942",
                unitId = "UNIDAD-504",
                torreId = "TORRE-CENTRAL-01",
                createdAt = now - 7200000,
                updatedAt = now - 2400000
            ),
            Pickup(
                id = "REC-803",
                trackingCode = "REC-803-STF",
                clientName = "TechStore Logística Ecommerce",
                contactPerson = "Eduardo Ramos",
                phone = "+52 55 8765 4321",
                address = "Prolongación Paseo de la Reforma 1200, Santa Fe, CDMX",
                reference = "Sótano 2, área de paquetería express",
                timeWindow = "14:00 - 15:30 hrs",
                packagesCount = 8,
                totalWeightKg = 18.2,
                packageType = "Electrónicos y laptops",
                priority = Priority.NORMAL,
                status = PickupStatus.ACEPTADA,
                notes = "Contienen baterías de litio. Manejo delicado.",
                specialInstructions = "Escanear cada código de barra al subir a la unidad.",
                driverId = "DRV-9942",
                unitId = "UNIDAD-504",
                torreId = "TORRE-CENTRAL-01",
                createdAt = now - 10800000,
                updatedAt = now - 3600000
            ),
            Pickup(
                id = "REC-800",
                trackingCode = "REC-800-NAP",
                clientName = "Laboratorios Bioquímicos San Ángel",
                contactPerson = "Dra. Patricia Luna",
                phone = "+52 55 2345 6789",
                address = "Av. Revolución 980, Col. San José Insurgentes, CDMX",
                reference = "Recepción de muestras, Piso 1",
                timeWindow = "08:00 - 09:00 hrs",
                packagesCount = 2,
                totalWeightKg = 5.0,
                packageType = "Muestras diagnósticas",
                priority = Priority.NORMAL,
                status = PickupStatus.RECOLECTADA,
                notes = "Recolectado sin novedades a las 08:42 hrs.",
                signatureName = "Patricia Luna R.",
                hasSignature = true,
                evidenceNotes = "2 paquetes sellados con cinta de seguridad intacta.",
                driverId = "DRV-9942",
                unitId = "UNIDAD-504",
                torreId = "TORRE-CENTRAL-01",
                createdAt = now - 18000000,
                updatedAt = now - 14400000
            )
        )
    }

    private fun getInitialTowerMessages(): List<TowerMessage> {
        val now = System.currentTimeMillis()
        return listOf(
            TowerMessage(
                id = "MSG-1",
                sender = "Torre Central (Despacho)",
                message = "Unidad 504 enlazada correctamente al canal de recolecciones prioritarias.",
                timestamp = now - 7200000,
                isUrgent = false
            ),
            TowerMessage(
                id = "MSG-2",
                sender = "Torre Central (Monitor Ruta)",
                message = "Alerta vial: Tráfico denso sobre Periférico Sur a la altura de Barranca del Muerto.",
                timestamp = now - 3600000,
                isUrgent = true
            ),
            TowerMessage(
                id = "MSG-3",
                sender = "Torre Central (Operaciones)",
                message = "Cliente Médica del Valle confirmó que sus paquetes ya están en rampa listos para entrega.",
                timestamp = now - 1800000,
                isUrgent = false
            )
        )
    }

    fun setIncomingAlert(pickup: Pickup?) {
        _incomingPickupAlert.value = pickup
    }

    fun acceptIncomingPickup(pickupId: String) {
        val alert = _incomingPickupAlert.value
        if (alert != null && alert.id == pickupId) {
            val accepted = alert.copy(
                status = PickupStatus.ACEPTADA,
                updatedAt = System.currentTimeMillis(),
                driverId = _driverProfile.value.id,
                unitId = _driverProfile.value.unitId
            )
            _pickups.update { current -> listOf(accepted) + current }
            _incomingPickupAlert.value = null
            syncToTower(accepted)
        } else {
            updatePickupStatus(pickupId, PickupStatus.ACEPTADA)
        }
    }

    fun rejectIncomingPickup(pickupId: String, reason: String = "Capacidad llena en unidad") {
        val alert = _incomingPickupAlert.value
        if (alert != null && alert.id == pickupId) {
            val rejected = alert.copy(
                status = PickupStatus.RECHAZADA,
                incidentReason = reason,
                updatedAt = System.currentTimeMillis()
            )
            _incomingPickupAlert.value = null
            syncToTower(rejected)
            addTowerMessage("Rechazo de recolección ${rejected.trackingCode} reportado a Torre: $reason", isUrgent = true)
        }
    }

    fun updatePickupStatus(pickupId: String, newStatus: PickupStatus, notes: String = "") {
        _pickups.update { current ->
            current.map { item ->
                if (item.id == pickupId) {
                    val updated = item.copy(
                        status = newStatus,
                        updatedAt = System.currentTimeMillis(),
                        notes = if (notes.isNotBlank()) notes else item.notes
                    )
                    syncToTower(updated)
                    updated
                } else item
            }
        }
    }

    fun completePickup(
        pickupId: String,
        signerName: String,
        evidenceNotes: String,
        hasSignature: Boolean
    ) {
        _pickups.update { current ->
            current.map { item ->
                if (item.id == pickupId) {
                    val updated = item.copy(
                        status = PickupStatus.RECOLECTADA,
                        signatureName = signerName,
                        hasSignature = hasSignature,
                        evidenceNotes = evidenceNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                    syncToTower(updated)
                    addTowerMessage("Recolección ${item.trackingCode} finalizada con éxito. Firma registrada de: $signerName")
                    updated
                } else item
            }
        }
    }

    fun reportIncident(pickupId: String, reason: String, notes: String) {
        _pickups.update { current ->
            current.map { item ->
                if (item.id == pickupId) {
                    val updated = item.copy(
                        status = PickupStatus.INCIDENCIA,
                        incidentReason = reason,
                        evidenceNotes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    syncToTower(updated)
                    addTowerMessage("INCIDENCIA en ${item.trackingCode}: $reason ($notes)", isUrgent = true)
                    updated
                } else item
            }
        }
    }

    fun updateDriverStatus(newStatus: DriverStatus) {
        _driverProfile.update { it.copy(status = newStatus) }
        scope.launch {
            firebaseManager.saveDriverPing(_driverProfile.value)
        }
    }

    fun updateTowerId(newTorreId: String) {
        val cleanId = newTorreId.trim().uppercase()
        _driverProfile.update { it.copy(currentTorreId = cleanId) }
        _towerConnection.update {
            it.copy(
                torreId = cleanId,
                channel = "firestore://torres/$cleanId/recolecciones",
                lastPingTime = System.currentTimeMillis(),
                isLinked = true
            )
        }
        scope.launch {
            firebaseManager.saveDriverPing(_driverProfile.value)
        }
        addTowerMessage("Enlace actualizado exitosamente con la torre: $cleanId")
    }

    fun updateDriverProfile(name: String, unitId: String, plate: String, phone: String) {
        _driverProfile.update {
            it.copy(
                name = name,
                unitId = unitId,
                licensePlate = plate,
                phone = phone
            )
        }
        scope.launch {
            firebaseManager.saveDriverPing(_driverProfile.value)
        }
    }

    fun triggerSimulatedDispatchFromTower() {
        val randId = (100..999).random()
        val clients = listOf(
            Triple("Almacenes Centrales del Bajío", "Av. Ceylán 430, Azcapotzalco", "7 cajas de refacciones (45kg)"),
            Triple("Importadora Farmacéutica Global", "Calzada de Tlalpan 2100, Coyoacán", "3 cajas con medicina general (14kg)"),
            Triple("Logística & Paquetes NextDay", "Blvd. Manuel Ávila Camacho 190, Polanco", "15 paquetes de retail e-commerce (28kg)"),
            Triple("Textiles & Telas Metropolitanas", "Eje Central Lázaro Cárdenas 620, Doctores", "4 rollos de tela especial (62kg)")
        )
        val selected = clients.random()
        val newPickup = Pickup(
            id = "REC-$randId",
            trackingCode = "REC-$randId-DESP",
            clientName = selected.first,
            contactPerson = "Responsable de Despacho",
            phone = "+52 55 ${(1000..9999).random()} ${(1000..9999).random()}",
            address = selected.second,
            reference = "Rampa de carga principal. Presentar ID de conductor.",
            timeWindow = "Inmediata / Prioridad",
            packagesCount = (2..12).random(),
            totalWeightKg = (10..80).random().toDouble(),
            packageType = selected.third,
            priority = if (randId % 2 == 0) Priority.URGENTE else Priority.ALTA,
            status = PickupStatus.PENDIENTE,
            notes = "Asignación en vivo enviada desde la Torre de Control.",
            specialInstructions = "Confirmar recepción inmediatamente en la aplicación.",
            driverId = _driverProfile.value.id,
            unitId = _driverProfile.value.unitId,
            torreId = _towerConnection.value.torreId,
            createdAt = System.currentTimeMillis()
        )

        _incomingPickupAlert.value = newPickup
        addTowerMessage("¡Nueva recolección ${newPickup.trackingCode} asignada por Torre Central!", isUrgent = true)
        syncToTower(newPickup)
    }

    suspend fun loginWithGoogleToken(idToken: String): Boolean {
        _authError.value = null
        val result = firebaseManager.signInWithGoogleCredential(idToken)
        return if (result.isSuccess) {
            val user = result.getOrNull()
            if (user != null) {
                _driverProfile.update {
                    it.copy(
                        name = user.displayName ?: it.name,
                        email = user.email ?: it.email
                    )
                }
            }
            _isAuthenticated.value = true
            scope.launch { firebaseManager.saveDriverPing(_driverProfile.value) }
            true
        } else {
            _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Error de autenticación con Google"
            false
        }
    }

    suspend fun loginWithEmail(email: String, pass: String): Boolean {
        _authError.value = null
        val result = firebaseManager.signInWithEmail(email, pass)
        return if (result.isSuccess) {
            val user = result.getOrNull()
            if (user != null) {
                _driverProfile.update {
                    it.copy(
                        name = user.displayName ?: it.name,
                        email = user.email ?: email
                    )
                }
            }
            _isAuthenticated.value = true
            scope.launch { firebaseManager.saveDriverPing(_driverProfile.value) }
            true
        } else {
            _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Credenciales no válidas"
            false
        }
    }

    fun logout() {
        firebaseManager.signOut()
        _isAuthenticated.value = false
        _authError.value = null
    }

    private fun addTowerMessage(text: String, isUrgent: Boolean = false) {
        val msg = TowerMessage(
            id = UUID.randomUUID().toString(),
            sender = "Torre Central",
            message = text,
            timestamp = System.currentTimeMillis(),
            isUrgent = isUrgent
        )
        _towerMessages.update { listOf(msg) + it }
    }

    private fun syncToTower(pickup: Pickup) {
        _towerConnection.update { it.copy(lastPingTime = System.currentTimeMillis()) }
        scope.launch {
            firebaseManager.updatePickupInFirestore(pickup)
            firebaseManager.saveDriverPing(_driverProfile.value)
        }
    }
}
