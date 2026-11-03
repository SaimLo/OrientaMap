package com.showroomprive.orientamap.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MapPlaceholderScreen(
    projectId: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Écran carte", style = MaterialTheme.typography.headlineSmall)
        Text("Projet : ${projectId ?: "inconnu"}")
        Text(
            text = "Le moteur cartographique, les couches IGN et le GPS seront intégrés dans le prochain lot.",
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("Retour")
        }
    }
}
