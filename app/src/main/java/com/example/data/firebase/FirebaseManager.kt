package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.DriverProfile
import com.example.data.model.Pickup
import com.example.data.model.PickupStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseManager(private val context: Context) {
    private val TAG = "FirebaseManager"

    val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = try {
            if (isFirebaseAvailable) FirebaseAuth.getInstance() else null
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not ready: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            if (isFirebaseAvailable) FirebaseFirestore.getInstance() else null
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not ready: ${e.message}")
            null
        }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser?> {
        return try {
            val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth no disponible"))
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signInWithGoogleCredential: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth no disponible"))
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signInWithEmail: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth no disponible"))
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signUpWithEmail: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signOut: ${e.message}")
        }
    }

    suspend fun saveDriverPing(driver: DriverProfile): Boolean {
        return try {
            val db = firestore ?: return false
            val data = hashMapOf(
                "driverId" to driver.id,
                "name" to driver.name,
                "email" to driver.email,
                "phone" to driver.phone,
                "unitId" to driver.unitId,
                "licensePlate" to driver.licensePlate,
                "status" to driver.status.name,
                "torreId" to driver.currentTorreId,
                "latitude" to driver.lastLatitude,
                "longitude" to driver.lastLongitude,
                "batteryPct" to driver.batteryPct,
                "lastPing" to System.currentTimeMillis()
            )
            db.collection("torres")
                .document(driver.currentTorreId)
                .collection("conductores")
                .document(driver.id)
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save driver ping to Firestore: ${e.message}")
            false
        }
    }

    suspend fun updatePickupInFirestore(pickup: Pickup): Boolean {
        return try {
            val db = firestore ?: return false
            val data = hashMapOf(
                "id" to pickup.id,
                "trackingCode" to pickup.trackingCode,
                "clientName" to pickup.clientName,
                "contactPerson" to pickup.contactPerson,
                "phone" to pickup.phone,
                "address" to pickup.address,
                "reference" to pickup.reference,
                "timeWindow" to pickup.timeWindow,
                "packagesCount" to pickup.packagesCount,
                "totalWeightKg" to pickup.totalWeightKg,
                "packageType" to pickup.packageType,
                "priority" to pickup.priority.name,
                "status" to pickup.status.name,
                "notes" to pickup.notes,
                "signatureName" to pickup.signatureName,
                "hasSignature" to pickup.hasSignature,
                "evidenceNotes" to pickup.evidenceNotes,
                "incidentReason" to pickup.incidentReason,
                "driverId" to pickup.driverId,
                "unitId" to pickup.unitId,
                "torreId" to pickup.torreId,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("torres")
                .document(pickup.torreId)
                .collection("recolecciones")
                .document(pickup.id)
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update pickup in Firestore: ${e.message}")
            false
        }
    }

    fun observeTowerPickups(torreId: String, driverId: String): Flow<List<Pickup>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("torres")
                .document(torreId)
                .collection("recolecciones")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val pickups = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val trackingCode = doc.getString("trackingCode") ?: "REC-${doc.id.take(4)}"
                                val clientName = doc.getString("clientName") ?: "Cliente"
                                val statusStr = doc.getString("status") ?: "PENDIENTE"
                                val status = try {
                                    PickupStatus.valueOf(statusStr)
                                } catch (_: Exception) {
                                    PickupStatus.PENDIENTE
                                }
                                Pickup(
                                    id = id,
                                    trackingCode = trackingCode,
                                    clientName = clientName,
                                    contactPerson = doc.getString("contactPerson") ?: "",
                                    phone = doc.getString("phone") ?: "",
                                    address = doc.getString("address") ?: "",
                                    reference = doc.getString("reference") ?: "",
                                    timeWindow = doc.getString("timeWindow") ?: "En espera",
                                    packagesCount = doc.getLong("packagesCount")?.toInt() ?: 1,
                                    totalWeightKg = doc.getDouble("totalWeightKg") ?: 0.0,
                                    packageType = doc.getString("packageType") ?: "Cajas",
                                    status = status,
                                    notes = doc.getString("notes") ?: "",
                                    signatureName = doc.getString("signatureName") ?: "",
                                    hasSignature = doc.getBoolean("hasSignature") ?: false,
                                    evidenceNotes = doc.getString("evidenceNotes") ?: "",
                                    incidentReason = doc.getString("incidentReason") ?: "",
                                    driverId = doc.getString("driverId") ?: driverId,
                                    torreId = torreId
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(pickups)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error registering firestore listener: ${e.message}")
        }

        awaitClose {
            listener?.remove()
        }
    }
}
