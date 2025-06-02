package com.example.kidguard

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EnfantRecordingActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var recorder: MediaRecorder? = null
    private var camera: Camera? = null
    private var surfaceView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null

    private var audioFilePath: String = ""
    private var videoFilePath: String = ""
    private var isRecording = false
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enfant_recording)

        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView?.holder
        surfaceHolder?.addCallback(this)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ),
            1
        )

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userId = user.uid
        listenToRecordingFlags(userId!!)
    }

    private fun listenToRecordingFlags(uid: String) {
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val ecoute = snapshot.getBoolean("ecoute") ?: false
                val cameraFlag = snapshot.getBoolean("camera") ?: false

                if (ecoute && !isRecording) startAudioRecording()
                if (cameraFlag && !isRecording) startVideoRecording()
            }
    }


    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission micro refusée", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "AUDIO_$timeStamp.m4a")
        audioFilePath = file.absolutePath

        recorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(audioFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
                start()
                isRecording = true
                Toast.makeText(this@EnfantRecordingActivity, "Enregistrement audio démarré", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EnfantRecordingActivity, "Erreur audio : ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                return
            }
        }

        Handler(mainLooper).postDelayed({ stopAudioRecording() }, 30000)
    }

    private fun stopAudioRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            Toast.makeText(this, "Enregistrement audio terminé", Toast.LENGTH_SHORT).show()
            uploadAudioToStorage(Uri.fromFile(File(audioFilePath)))
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur arrêt audio : ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            recorder = null
            isRecording = false

            userId?.let {
                db.collection("users").document(it).update("ecoute", false)
            }
        }
    }

    private fun uploadAudioToStorage(uri: Uri) {
        val filename = "recordings/audio/${userId}_${System.currentTimeMillis()}.m4a"
        storage.reference.child(filename)
            .putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(this, "Audio sauvegardé", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Échec de l'upload audio", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
            }
    }


    private fun startVideoRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            camera = Camera.open()
            camera?.apply {
                setDisplayOrientation(90)
                unlock()
            }

            val file = File(cacheDir, "VIDEO_${System.currentTimeMillis()}.mp4")
            videoFilePath = file.absolutePath

            recorder = MediaRecorder().apply {
                setCamera(camera)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(videoFilePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(640, 480)
                setVideoFrameRate(30)
                setOrientationHint(90)
                setPreviewDisplay(surfaceHolder?.surface)
                prepare()
                start()
            }

            isRecording = true
            Toast.makeText(this, "Enregistrement vidéo démarré", Toast.LENGTH_SHORT).show()
            Handler(mainLooper).postDelayed({ stopVideoRecording() }, 30000)

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur vidéo : ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            releaseCameraAndRecorder()
        }
    }

    private fun stopVideoRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            uploadVideoToStorage(Uri.fromFile(File(videoFilePath)))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
            isRecording = false

            camera?.apply {
                lock()
                stopPreview()
                release()
            }
            camera = null

            userId?.let {
                db.collection("users").document(it).update("camera", false)
            }
        }
    }

    private fun uploadVideoToStorage(uri: Uri) {
        val filename = "recordings/video/${userId}_${System.currentTimeMillis()}.mp4"
        storage.reference.child(filename)
            .putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(this, "Vidéo sauvegardée", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Échec de l'upload vidéo", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
            }
    }

    private fun releaseCameraAndRecorder() {
        recorder?.release()
        recorder = null
        camera?.release()
        camera = null
        isRecording = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface prête pour l'aperçu caméra
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface modifiée
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (isRecording) stopVideoRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCameraAndRecorder()
    }
}
