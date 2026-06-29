package com.glm.aiapp.ui.screens.vision

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.glm.aiapp.ui.components.LoadingState

@Composable
fun VisionScreen(vm: VisionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.setAttachment(it, context.contentResolver.getType(it)) }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { _ ->
        // Picture preview returns a Bitmap — for production, use TakePicture with a FileProvider URI.
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Analyze images, PDFs, and videos with Pullarao 1 Vision",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Preview area
        Surface(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (state.selectedUri != null) {
                AsyncImage(
                    model = state.selectedUri,
                    contentDescription = "Selected attachment",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Attachment, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No attachment selected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Pickers
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Attachment, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Pick image")
            }
            OutlinedButton(onClick = { pickImage.launch("*/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Pick file")
            }
        }

        // Prompt
        OutlinedTextField(
            value = state.prompt,
            onValueChange = vm::setPrompt,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Prompt") },
            minLines = 2,
            maxLines = 5
        )

        // Analyze
        Button(
            onClick = vm::analyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedUri != null && !state.isLoading
        ) {
            Icon(Icons.Filled.Analytics, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (state.isLoading) "Analyzing…" else "Analyze")
        }

        state.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        if (state.isLoading) {
            LoadingState(message = "GLM is looking at your attachment…")
        }

        state.analysis?.let { text ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Analysis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
