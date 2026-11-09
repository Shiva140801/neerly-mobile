package com.neerly.mobile.feature.driver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Takes a full-size photo into [File] through the system camera app.
 *
 * The manifest declares CAMERA, and Android refuses ACTION_IMAGE_CAPTURE from an app
 * that declares it without holding it — so the permission is asked for first, and
 * the camera opens once it is granted. A refusal reports `false` like a cancelled shot.
 */
@Composable
fun rememberCameraCapture(onResult: (ok: Boolean) -> Unit): (File) -> Unit {
    val context = LocalContext.current
    var target by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onResult)
    val launch: (File) -> Unit = { f ->
        runCatching { camera.launch(photoUri(context, f)) }.onFailure { onResult(false) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val f = target
        if (granted && f != null) launch(f) else onResult(false)
    }
    return { f ->
        target = f
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launch(f)
        else permission.launch(Manifest.permission.CAMERA)
    }
}
