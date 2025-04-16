// Fichier: RealtimeDatabaseSection.kt
package com.example.masuperappfirebase.ui.main // Assurez-vous que le package est correct

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Composable dédié à l'affichage et à l'interaction avec
 * la section message de la Realtime Database pour une référence donnée.
 *
 * @param userMessageReference La DatabaseReference pointant spécifiquement
 *                             vers le nœud 'message' de l'utilisateur actuel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeDatabaseSection(
    userMessageReference: DatabaseReference // Reçoit la référence comme paramètre
) {
    // --- États locaux pour ce composant ---
    var messageInput by remember { mutableStateOf("") }
    var messageFromDb by remember { mutableStateOf<String?>("Chargement...") }
    var isSaving by remember { mutableStateOf(false) }
    var dbError by remember { mutableStateOf<String?>(null) }

    // Scope Coroutine local
    val coroutineScope = rememberCoroutineScope()

    // --- Lire les données RTDB en temps réel ---
    // L'effet dépend maintenant de la référence passée en paramètre
    DisposableEffect(userMessageReference) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val message = snapshot.getValue(String::class.java)
                messageFromDb = message
                dbError = null
                Log.d("RTDBSection", "Data received for ref: ${snapshot.ref}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RTDBSection", "Database read failed for ref ${userMessageReference.toString()}: ${error.message}", error.toException())
                messageFromDb = null
                dbError = "Erreur lecture BDD: ${error.message}"
            }
        }
        Log.d("RTDBSection", "Adding ValueEventListener for path: ${userMessageReference.toString()}")
        userMessageReference.addValueEventListener(listener)

        onDispose {
            Log.d("RTDBSection", "Removing ValueEventListener for path: ${userMessageReference.toString()}")
            userMessageReference.removeEventListener(listener)
        }
    }

    // --- Interface Utilisateur de la Section RTDB ---
    Column(
        modifier = Modifier.fillMaxWidth(), // Prend toute la largeur disponible
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Realtime Database", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (dbError != null) {
            Text(dbError!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Message enregistré:")
        Text(
            text = messageFromDb ?: "(Aucun message)",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            label = { Text("Nouveau message") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (messageInput.isBlank()) return@Button
                isSaving = true
                dbError = null
                coroutineScope.launch {
                    try {
                        // Utilise la référence passée en paramètre
                        userMessageReference.setValue(messageInput).await()
                        Log.d("RTDBSection", "Message saved successfully to ${userMessageReference.toString()}!")
                        messageInput = ""
                    } catch (e: Exception) {
                        Log.e("RTDBSection", "Failed to save message to ${userMessageReference.toString()}", e)
                        dbError = "Erreur sauvegarde: ${e.localizedMessage}"
                    } finally {
                        isSaving = false
                    }
                }
            },
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Sauvegarder Message")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("🚨 N'oubliez pas les règles de sécurité RTDB !", style = MaterialTheme.typography.labelSmall)
    }
}