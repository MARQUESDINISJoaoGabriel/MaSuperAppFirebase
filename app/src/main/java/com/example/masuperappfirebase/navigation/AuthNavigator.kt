package com.example.masuperappfirebase.navigation

import com.example.masuperappfirebase.ui.main.HomeScreen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.masuperappfirebase.auth.AuthManager
import com.example.masuperappfirebase.ui.auth.AuthScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthNavigator() {
    // Récupère l'instance FirebaseAuth depuis notre 'manager' simplifié
    val auth = AuthManager.auth

    // État pour stocker l'utilisateur courant (null si déconnecté)
    // remember garde la valeur à travers les recompositions
    // mutableStateOf rend l'état observable par Compose
    var currentUser by remember { mutableStateOf(AuthManager.getCurrentUser()) }

    // Effet qui s'exécute une fois et nettoie en partant
    // Idéal pour enregistrer/désenregistrer des écouteurs
    DisposableEffect(auth) {
        // Crée un écouteur qui met à jour notre état 'currentUser'
        // quand l'état d'authentification de Firebase change
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            currentUser = user // Met à jour l'état, ce qui déclenche une recomposition
            Log.d("AuthNavigator", "Auth state changed, user: ${user?.uid}")
        }

        // Attache l'écouteur
        Log.d("AuthNavigator", "Adding AuthStateListener")
        auth.addAuthStateListener(authStateListener)

        // Le bloc 'onDispose' est appelé quand ce composable quitte l'écran
        // ou quand la clé 'auth' change (ne devrait pas arriver ici)
        onDispose {
            // Détache l'écouteur pour éviter les fuites de mémoire ! Très important.
            Log.d("AuthNavigator", "Removing AuthStateListener")
            auth.removeAuthStateListener(authStateListener)
        }
    }

    // Affiche l'écran approprié en fonction de l'état de connexion
    if (currentUser == null) {
        // Utilisateur non connecté -> Écran d'authentification
        AuthScreen(
            auth = auth,
            // Lambda appelée par AuthScreen quand la connexion/inscription réussit
            onAuthComplete = { user ->
                // Normalement, l'AuthStateListener met à jour 'currentUser'
                // mais on peut forcer ici si besoin (généralement pas nécessaire)
                // currentUser = user
                Log.d("AuthNavigator", "Auth complete for: ${user.uid}")
            }
        )
    } else {
        // Utilisateur connecté -> Écran principal
        HomeScreen(
            user = currentUser!!, // On sait que user n'est pas null ici
            // Lambda pour gérer la déconnexion depuis HomeScreen
            onLogout = {
                Log.d("AuthNavigator", "Logout requested")
                auth.signOut() // Déconnecte l'utilisateur
                // L'AuthStateListener détectera ce changement et mettra currentUser à null
            }
        )
    }
}