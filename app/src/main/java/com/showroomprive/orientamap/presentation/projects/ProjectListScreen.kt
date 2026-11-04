package com.showroomprive.orientamap.presentation.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.showroomprive.orientamap.domain.model.MapProject

@Composable
fun ProjectListScreen(
    projects: List<MapProject>,
    onCreateProject: () -> Unit,
    onOpenProject: (MapProject) -> Unit,
    onRenameProject: (MapProject, String) -> Unit,
    onDeleteProject: (MapProject) -> Unit
) {
    var projectToRename by remember { mutableStateOf<MapProject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Projets cartographiques",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onCreateProject,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_project_button")
        ) {
            Text("Créer un projet")
        }

        if (projects.isEmpty()) {
            Text("Aucun projet. Crée ton premier projet pour commencer.")
        } else {
            LazyColumn(
                modifier = Modifier.testTag("project_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_card_${project.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            project.description?.let { Text(it) }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { onOpenProject(project) },
                                    modifier = Modifier.testTag("open_project_button_${project.id}")
                                ) {
                                    Text("Ouvrir")
                                }
                                TextButton(
                                    onClick = { projectToRename = project },
                                    modifier = Modifier.testTag("rename_project_button_${project.id}")
                                ) {
                                    Text("Renommer")
                                }
                                TextButton(
                                    onClick = { onDeleteProject(project) },
                                    modifier = Modifier.testTag("delete_project_button_${project.id}")
                                ) {
                                    Text("Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    projectToRename?.let { project ->
        var newName by remember(project) { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Renommer le projet") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom du projet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameProject(project, trimmed)
                            projectToRename = null
                        }
                    },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToRename = null },
                    modifier = Modifier.testTag("rename_cancel_button")
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
