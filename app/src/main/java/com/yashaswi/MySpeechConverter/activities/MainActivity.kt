package com.yashaswi.MySpeechConverter.activities

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yashaswi.MySpeechConverter.*
import com.yashaswi.MySpeechConverter.dtos.SpokenTextDTO
import com.yashaswi.MySpeechConverter.googlespeech.SpeechService
import com.yashaswi.MySpeechConverter.googlespeech.VoiceRecorder
import com.yashaswi.MySpeechConverter.realm.RealmHelper
import com.yashaswi.MySpeechConverter.utilities.Constants
import com.yashaswi.MySpeechConverter.utilities.Constants.MY_PERMISSIONS_REQUEST_RECORD_AUDIO
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_item.view.*


class MainActivity : AppCompatActivity() {

    private var audioBtnState = Constants.START
    private lateinit var mVoiceRecorder: VoiceRecorder
    private lateinit var mSpeechService: SpeechService
    private var mAdapter: ResultAdapter? = null
    private var TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    /**
     * Initializes the views
     */
    private fun initView() {
        // Prepare Cloud Speech API
        bindService(Intent(this, SpeechService::class.java), mServiceConnection, BIND_AUTO_CREATE)
        // Ask for audio record permission
        showPermissionMessageDialog()
        spokenTextRV.layoutManager = LinearLayoutManager(this)
        mAdapter = ResultAdapter(
            RealmHelper(Realm.getDefaultInstance())
                .retrieve()
        )
        spokenTextRV.adapter = mAdapter
        recordBtn.setOnClickListener {
            if (audioBtnState == Constants.START)
                startRecording()
            else if (audioBtnState == Constants.STOP)
                stopRecording()
        }
    }

    /**
     * Voice record callback method returned once the user starts speaking
     */
    private val mVoiceCallback: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
            mSpeechService.startRecognizing(mVoiceRecorder.sampleRate)
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            mSpeechService.recognize(data, size)
        }

        override fun onVoiceEnd() {
            mSpeechService.finishRecognizing()
        }
    }

    /**
     * establishes the speech service  connections and returns the callbacks with the result
     */
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            mSpeechService =
                SpeechService.from(binder)
            mSpeechService.addListener(mSpeechServiceListener)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            //  do nothing
        }
    }

    /**
     * Returns the callback once the speech is converted to text
     */
    private val mSpeechServiceListener: SpeechService.Listener =
        SpeechService.Listener { text, isFinal ->
            if (isFinal) {
                mVoiceRecorder.dismiss()
            }
            if (spokenTextTV != null && !TextUtils.isEmpty(text)) {
                runOnUiThread {
                    if (isFinal) {
                        spokenTextTV.text = null
                        mAdapter?.addResult(text)
                        spokenTextRV.smoothScrollToPosition(0)
                    } else {
                        spokenTextTV.text = text
                    }
                }
            }
        }


    private fun stopRecording() {
        audioBtnState = Constants.START
        recordStatusTV.text = resources.getString(R.string.startToRecordTxt)
        recordBtn.background = resources.getDrawable(R.drawable.ic_mic_on, null)
        mVoiceRecorder.stop()
    }

    private fun startRecording() {
        audioBtnState = Constants.STOP
        recordStatusTV.text = resources.getString(R.string.stopRecordTxt)
        recordBtn.background = resources.getDrawable(R.drawable.ic_mic_off, null)
        mVoiceRecorder =
            VoiceRecorder(mVoiceCallback)
        mVoiceRecorder.start()
    }

    override fun onStop() {

        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener)
        //unbind the speechService
        unbindService(mServiceConnection)
        Realm.getDefaultInstance().close()
        super.onStop()

    }

    /**
     * permission request dialog to record the audio
     */
    private fun showPermissionMessageDialog() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied")
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Permission to access the microphone is required for this app to record audio.")
                    .setTitle("Permission required")

                builder.setPositiveButton(
                    "OK"
                ) { dialog, id ->
                    Log.i(TAG, "Clicked")
                    makeRequest()
                }

                val dialog = builder.create()
                dialog.show()
            } else {
                makeRequest()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
    }

    /**
     * Requesting for permission
     */
    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO
        )
    }


    /**
     * RecyclerView viewholder
     */
    class SpeechViewHolder internal constructor(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.layout_item, parent, false)) {
        var suggestionText: TextView = itemView.spokenTextItemTV
        var removeTextBtn: ImageButton = itemView.spokenTextRemoveIB

    }


    /**
     * RecyclerView adapter class
     */
    inner class ResultAdapter internal constructor(results: ArrayList<String>?) :
        RecyclerView.Adapter<SpeechViewHolder?>() {
        private var allSpokenTexts = ArrayList<String>()

        init {
            results?.let { allSpokenTexts.addAll(it) }
        }

        fun addResult(result: String) {
            val spokenTexts = SpokenTextDTO()
            spokenTexts.id = System.currentTimeMillis().toInt()
            spokenTexts.spokenText = result
            RealmHelper(Realm.getDefaultInstance())
                .save(spokenTexts)
            allSpokenTexts.add(0, result)
            stopRecording()
            notifyDataSetChanged()
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeechViewHolder {
            return SpeechViewHolder(
                LayoutInflater.from(parent.context),
                parent
            )
        }


        override fun getItemCount(): Int {
            return allSpokenTexts.size
        }

        override fun onBindViewHolder(holder: SpeechViewHolder, position: Int) {
            holder.suggestionText.text = allSpokenTexts[position]
        }
    }

}
