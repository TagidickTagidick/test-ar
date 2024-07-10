package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import io.github.sceneview.SceneView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val kModelFile = "models/damaged_helmet.glb"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                MyApp()
            }
        }
    }

    @Composable
    fun MyApp() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val recorder = remember { MediaRecorder() }
        val sceneView = remember { SceneView(context) }

//        val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
//        val audioPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
//        val storagePermissionState = rememberPermissionState(permission = Manifest.permission.WRITE_EXTERNAL_STORAGE)

        var isRecording by remember { mutableStateOf(false) }
        var isLongPress by remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (isRecording) {
                        stopRecording(recorder)
                        isRecording = false
                    } else {
                        takeScreenshot(context, sceneView)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
//                onLongClick = {
//                    coroutineScope.launch {
//                        if (!isRecording) {
//                            if (cameraPermissionState.hasPermission &&
//                                audioPermissionState.hasPermission &&
//                                storagePermissionState.hasPermission) {
//                                startRecording(recorder)
//                                isRecording = true
//                            } else {
//                                cameraPermissionState.launchPermissionRequest()
//                                audioPermissionState.launchPermissionRequest()
//                                storagePermissionState.launchPermissionRequest()
//                            }
//                        } else {
//                            stopRecording(recorder)
//                            isRecording = false
//                        }
//                    }
//                }
            ) {
                Text(if (isRecording) "Stop Recording" else "Take Screenshot")
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                ARSceneView(sceneView)
            }
        }
    }

    @Composable
    fun ARSceneView(sceneView: SceneView) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val cameraNode = rememberARCameraNode(engine)
        val childNodes = rememberNodes()
        val view = rememberView(engine)
        val collisionSystem = rememberCollisionSystem(view)

        var planeRenderer by remember { mutableStateOf(true) }

        var trackingFailureReason by remember {
            mutableStateOf<TrackingFailureReason?>(null)
        }
        var frame by remember { mutableStateOf<Frame?>(null) }

        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            cameraNode = cameraNode,
            planeRenderer = planeRenderer,
            onTrackingFailureChanged = {
                trackingFailureReason = it
            },
            onSessionUpdated = { session, updatedFrame ->
                frame = updatedFrame

//                if (childNodes.isEmpty()) {
//                    updatedFrame.getUpdatedPlanes()
//                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
//                        ?.let { it.createAnchorOrNull(it.centerPose) }?.let { anchor ->
//                            childNodes += createAnchorNode(
//                                engine = engine,
//                                modelLoader = modelLoader,
//                                materialLoader = materialLoader,
//                                anchor = anchor
//                            )
//                        }
//                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, node ->
                    if (node == null) {
                        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                        hitResults?.firstOrNull {
                            it.isValid(
                                depthPoint = false,
                                point = false
                            )
                        }?.createAnchorOrNull()
                            ?.let { anchor ->
                                planeRenderer = false
                                childNodes += createAnchorNode(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    anchor = anchor
                                )
                            }
                    }
                })
        )

//        Text(
//            modifier = Modifier
//                .systemBarsPadding()
//                .fillMaxWidth()
//                .align(Alignment.TopCenter)
//                .padding(top = 16.dp, start = 32.dp, end = 32.dp),
//            textAlign = TextAlign.Center,
//            fontSize = 28.sp,
//            color = Color.White,
//            text = trackingFailureReason?.let {
//                it.getDescription(LocalContext.current)
//            } ?: if (childNodes.isEmpty()) {
//                stringResource(R.string.point_your_phone_down)
//            } else {
//                stringResource(R.string.tap_anywhere_to_add_model)
//            }
//        )
    }

    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        anchor: Anchor
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(kModelFile),
            // Scale to fit in a 0.5 meters cube
            scaleToUnits = 0.5f
        ).apply {
            // Model Node needs to be editable for independent rotation from the anchor rotation
            isEditable = true
            editableScaleRange = 0.2f..0.75f
        }
        val boundingBoxNode = CubeNode(
            engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
        ).apply {
            isVisible = false
        }
        modelNode.addChildNode(boundingBoxNode)
        anchorNode.addChildNode(modelNode)

        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
            }
        }
        return anchorNode
    }

    private fun takeScreenshot(context: Context, sceneView: SceneView) {
        val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        sceneView.draw(canvas)

        val file = File(context.getExternalFilesDir(null), "screenshot.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Notify the user about the screenshot location
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startRecording(context: Context, recorder: MediaRecorder) {
        val file = File(context.getExternalFilesDir(null), "recording.mp4")

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(1920, 1080)
            setVideoFrameRate(30)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording(recorder: MediaRecorder) {
        recorder.apply {
            stop()
            reset()
            release()
        }
    }
    }