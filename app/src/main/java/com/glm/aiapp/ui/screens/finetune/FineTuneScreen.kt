package com.glm.aiapp.ui.screens.finetune

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glm.aiapp.domain.model.FineTuneJob
import com.glm.aiapp.domain.model.FineTuneStatus
import com.glm.aiapp.ui.components.EmptyState

@Composable
fun FineTuneScreen(vm: FineTuneViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val datasets by vm.datasets.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Fine-tune Studio — manage datasets and training jobs for your custom Pullarao models.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Datasets
        Text("Datasets", style = MaterialTheme.typography.titleMedium)
        if (datasets.isEmpty()) {
            Text("No datasets yet. Upload datasets via API or import to seed this list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            datasets.forEach { ds ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ds.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${ds.examples} examples · ${ds.format.uppercase()} · ${ds.sizeBytes / 1024} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = state.newDatasetId == ds.id, onClick = { vm.setDataset(ds.id) })
                    }
                }
            }
        }

        HorizontalDivider()

        // New job form
        Text("New job", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.newJobName,
            onValueChange = vm::setName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Job name") },
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Pullarao 1", "Pullarao 1 Lite", "Pullarao 1 Plus", "Pullarao 1 Air", "Pullarao 1 Flash").forEach { m ->
                FilterChip(
                    selected = state.newBaseModel == m,
                    onClick = { vm.setBaseModel(m) },
                    label = { Text(m) }
                )
            }
        }
        Text("Epochs: ${state.newEpochs}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = state.newEpochs.toFloat(),
            onValueChange = { vm.setEpochs(it.toInt()) },
            valueRange = 1f..20f,
            steps = 18
        )
        Text("Learning rate: ${state.newLearningRate}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = state.newLearningRate,
            onValueChange = { vm.setLearningRate(it) },
            valueRange = 1e-6f..1e-4f
        )
        Button(
            onClick = vm::createJob,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.newJobName.isNotBlank() && state.newDatasetId.isNotBlank()
        ) { Text("Queue job") }

        state.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        HorizontalDivider()

        // Jobs
        Text("Jobs", style = MaterialTheme.typography.titleMedium)
        if (jobs.isEmpty()) {
            EmptyState(
                title = "No jobs yet",
                subtitle = "Queue a job above to start training your custom Pullarao."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(jobs, key = { it.id }) { job -> JobCard(job, onRefresh = { vm.refresh(job.id) }) }
            }
        }
    }
}

@Composable
private fun JobCard(job: FineTuneJob, onRefresh: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = onRefresh,
                    label = { Text(job.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (job.status) {
                            FineTuneStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            FineTuneStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            FineTuneStatus.TRAINING -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Base: ${job.baseModel} · Dataset: ${job.datasetName} (${job.examples} examples)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Epochs: ${job.epochs} · LR: ${job.learningRate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (job.status == FineTuneStatus.TRAINING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth())
                Text("Progress: ${(job.progress * 100).toInt()}%${job.loss?.let { " · Loss: ${"%.4f".format(it)}" } ?: ""}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
