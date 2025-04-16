package com.example.masuperappfirebase.ui.auth // Créez ce package

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(auth: FirebaseAuth, onAuthComplete: (FirebaseUser) -> Unit) {

    // États pour les champs et l'UI
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Scope pour lancer les opérations asynchrones (appels Firebase)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- Interface Utilisateur (Compose UI) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenue !", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Affiche les erreurs
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Affiche indicateur de chargement OU les boutons
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Bouton Connexion Email/Pass
                Button(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email et mot de passe requis."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = auth.signInWithEmailAndPassword(email, password).await()
                            Log.d("AuthScreen", "Login Success: ${result.user?.email}")
                            onAuthComplete(result.user!!)
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            Log.w("AuthScreen", "Login Failed: Invalid Credentials", e)
                            errorMessage = "Email ou mot de passe incorrect."
                        } catch (e: Exception) {
                            Log.w("AuthScreen", "Login Failed", e)
                            errorMessage = "Échec connexion: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                }) { Text("Connexion") }

                // Bouton Inscription Email/Pass
                Button(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email et mot de passe requis."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = auth.createUserWithEmailAndPassword(email, password).await()
                            Log.d("AuthScreen", "Sign Up Success: ${result.user?.email}")
                            onAuthComplete(result.user!!) // Connecte direct après inscription
                        } catch (e: FirebaseAuthUserCollisionException) {
                            Log.w("AuthScreen", "Sign Up Failed: Email already in use.", e)
                            errorMessage = "Cet email est déjà utilisé."
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            Log.w("AuthScreen", "Sign Up Failed: Weak password.", e)
                            errorMessage = "Mot de passe trop faible (6 caractères min)."
                        } catch (e: Exception) {
                            Log.w("AuthScreen", "Sign Up Failed", e)
                            errorMessage = "Échec inscription: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                }) { Text("Inscription") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}