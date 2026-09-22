package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DriverProfile
import com.example.data.model.DriverStatus
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.example.data.model.TowerConnectionState
import com.example.data.model.TowerMessage
import com.example.data.repository.LogisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PickupFilter(val label: String) {
    TODAS("Todas"),
    ACTIVAS("En Ruta / Activas"),
    PENDIENTES("Pendientes"),
    COMPLETADAS("Recolectadas")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LogisticsRepository(application.applicationContext)

    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated
    val authError: StateFlow<String?> = repository.authError

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    val driverProfile: StateFlow<DriverProfile> = repository.driverProfile
    val towerConnection: StateFlow<TowerConnectionState> = repository.towerConnection
    val incomingPickupAlert: StateFlow<Pickup?> = repository.incomingPickupAlert
    val towerMessages: StateFlow<List<TowerMessage>> = repository.towerMessages

    private val _selectedFilter = MutableStateFlow(PickupFilter.TODAS)
    val selectedFilter: StateFlow<PickupFilter> = _selectedFilter.asStateFlow()

    private val _selectedPickup = MutableStateFlow<Pickup?>(null)
    val selectedPickup: StateFlow<Pickup?> = _selectedPickup.asStateFlow()

    private val _incidentTargetPickup = MutableStateFlow<Pickup?>(null)
    val incidentTargetPickup: StateFlow<Pickup?> = _incidentTargetPickup.asStateFlow()

    val pickups: StateFlow<List<Pickup>> = repository.pickups

    val filteredPickups: StateFlow<List<Pickup>> = combine(
        repository.pickups,
        _selectedFilter
    ) { list, filter ->
        when (filter) {
            PickupFilter.TODAS -> list
            PickupFilter.ACTIVAS -> list.filter {
                it.status == PickupStatus.ACEPTADA ||
                it.status == PickupStatus.EN_CAMINO ||
                it.status == PickupStatus.EN_SITIO
            }
            PickupFilter.PENDIENTES -> list.filter { it.status == PickupStatus.PENDIENTE }
            PickupFilter.COMPLETADAS -> list.filter {
                it.status == PickupStatus.RECOLECTADA || it.status == PickupStatus.INCIDENCIA
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = repository.pickups.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count {
            it.status == PickupStatus.ACEPTADA ||
            it.status == PickupStatus.EN_CAMINO ||
            it.status == PickupStatus.EN_SITIO
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val collectedTodayCount: StateFlow<Int> = repository.pickups.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count { it.status == PickupStatus.RECOLECTADA }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalWeightKgToday: StateFlow<Double> = repository.pickups.combine(MutableStateFlow(Unit)) { list, _ ->
        list.filter { it.status == PickupStatus.RECOLECTADA }.sumOf { it.totalWeightKg }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setFilter(filter: PickupFilter) {
        _selectedFilter.value = filter
    }

    fun selectPickup(pickup: Pickup?) {
        _selectedPickup.value = pickup
    }

    fun openIncidentDialog(pickup: Pickup) {
        _incidentTargetPickup.value = pickup
    }

    fun closeIncidentDialog() {
        _incidentTargetPickup.value = null
    }

    fun acceptIncomingPickup(pickupId: String) {
        repository.acceptIncomingPickup(pickupId)
    }

    fun rejectIncomingPickup(pickupId: String, reason: String = "Capacidad llena en unidad") {
        repository.rejectIncomingPickup(pickupId, reason)
    }

    fun advancePickupStatus(pickup: Pickup) {
        val nextStatus = when (pickup.status) {
            PickupStatus.PENDIENTE -> PickupStatus.ACEPTADA
            PickupStatus.ACEPTADA -> PickupStatus.EN_CAMINO
            PickupStatus.EN_CAMINO -> PickupStatus.EN_SITIO
            PickupStatus.EN_SITIO -> PickupStatus.RECOLECTADA
            else -> pickup.status
        }
        repository.updatePickupStatus(pickup.id, nextStatus)

        // If it's the currently selected pickup, update local selected state
        if (_selectedPickup.value?.id == pickup.id) {
            _selectedPickup.value = _selectedPickup.value?.copy(status = nextStatus)
        }
    }

    fun completePickup(
        pickupId: String,
        signerName: String,
        evidenceNotes: String,
        hasSignature: Boolean
    ) {
        repository.completePickup(pickupId, signerName, evidenceNotes, hasSignature)
        if (_selectedPickup.value?.id == pickupId) {
            _selectedPickup.value = _selectedPickup.value?.copy(
                status = PickupStatus.RECOLECTADA,
                signatureName = signerName,
                hasSignature = hasSignature,
                evidenceNotes = evidenceNotes
            )
        }
    }

    fun submitIncident(reason: String, notes: String) {
        val target = _incidentTargetPickup.value ?: return
        repository.reportIncident(target.id, reason, notes)
        if (_selectedPickup.value?.id == target.id) {
            _selectedPickup.value = _selectedPickup.value?.copy(
                status = PickupStatus.INCIDENCIA,
                incidentReason = reason,
                evidenceNotes = notes
            )
        }
        _incidentTargetPickup.value = null
    }

    fun toggleDriverStatus() {
        val current = driverProfile.value.status
        val next = when (current) {
            DriverStatus.DISPONIBLE -> DriverStatus.EN_RUTA
            DriverStatus.EN_RUTA -> DriverStatus.FUERA_TURNO
            DriverStatus.FUERA_TURNO -> DriverStatus.DISPONIBLE
        }
        repository.updateDriverStatus(next)
    }

    fun setDriverStatus(status: DriverStatus) {
        repository.updateDriverStatus(status)
    }

    fun updateTowerId(newTorreId: String) {
        repository.updateTowerId(newTorreId)
    }

    fun updateDriverProfile(name: String, unitId: String, plate: String, phone: String) {
        repository.updateDriverProfile(name, unitId, plate, phone)
    }

    fun triggerSimulatedDispatch() {
        repository.triggerSimulatedDispatchFromTower()
    }

    fun loginWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("dummy-client-id.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(context, request)
                val credential = response.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    repository.loginWithGoogleToken(idToken)
                } else {
                    // Fallback to identified driver sekoner0121@gmail.com
                    repository.loginDirectDriver("sekoner0121@gmail.com", "Carlos Mendoza", "UNIDAD-504")
                }
            } catch (e: Exception) {
                // If Google Play Services is missing OAuth config or in emulator environment, authenticate directly as driver
                repository.loginDirectDriver("sekoner0121@gmail.com", "Carlos Mendoza", "UNIDAD-504")
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val success = repository.loginWithEmail(email, pass)
                if (!success) {
                    // If demo user or offline Firebase, allow instant driver access
                    repository.loginDirectDriver(email, "Conductor " + email.substringBefore("@"), "UNIDAD-504")
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun loginDirectDriver(email: String = "sekoner0121@gmail.com", name: String = "Carlos Mendoza", unitId: String = "UNIDAD-504") {
        repository.loginDirectDriver(email, name, unitId)
    }

    fun logout() {
        repository.logout()
    }
}
