package espinoza.hector.mypokedex_espinozahector

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class RegisterPokemonActivity : AppCompatActivity() {

    val REQUEST_IMAGE_GET = 1
    val CLOUD_NAME = "dlcv1adru"
    val UPLOAD_PRESET = "pokemon-upload"
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_pokemon)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initCloudinary()

        val name: EditText = findViewById(R.id.pokemonName) as EditText
        val number: EditText = findViewById(R.id.pokemonNumber) as EditText
        val select: Button = findViewById(R.id.selectImage) as Button
        val save: Button = findViewById(R.id.savePokemon) as Button

        select.setOnClickListener {
            val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }

        save.setOnClickListener {
            val nombre = name.text.toString()
            val numero = number.text.toString()

            if (nombre.isNotEmpty() && numero.isNotEmpty()) {
                savePokemon { imageUrl ->
                    savePokemonToDatabase(nombre, numero, imageUrl)
                }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK){
            val fullImageUri: Uri? = data?.data

            if (fullImageUri != null){
                changeImage(fullImageUri)
            }
        }
    }

    fun changeImage(uri: Uri){
        val thumbnail: ImageView = findViewById(R.id.thumbnail) as ImageView
        imageUri = uri
        try {
            thumbnail.setImageURI(uri)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun initCloudinary () {
        val config: MutableMap <String, String> = HashMap <String, String>()
        config["cloud_name"] = CLOUD_NAME
        MediaManager.init(this, config)
    }

    fun savePokemon(callback: (String) -> Unit){
        var url: String = ""

        if (imageUri != null){
            MediaManager.get().upload(imageUri).unsigned(UPLOAD_PRESET).callback(object: UploadCallback {
                override fun onStart(requesId: String) {
                    Log.d("Cloudinary Quickstart", "Upload start")
                }
                override fun onProgress (requesId: String, bytes: Long, totalBytes: Long) {
                    Log.d("Cloudinary Quickstart", "Upload progress")
                }
                override fun onSuccess (requestId: String, resultData: Map<*, *>){
                    Log.d("Cloudinary Quickstart", "Upload success")
                    url = resultData ["secure_url"] as String?:""
                    callback(url)
                }
                override fun onError (requesId: String, error: ErrorInfo) {
                    Log.d("Cloudinary Quickstart", "Upload failed")
                }
                override fun onReschedule (requesId: String, error: ErrorInfo){

                }
            }).dispatch()
        }

    }

    private fun savePokemonToDatabase(name: String, number: String, imageUrl: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference.child("Pokemns").child(number)
        val pokemon = Pokemon(name,
            number.toInt(),
            imageUrl)

        databaseRef.setValue(pokemon).addOnSuccessListener {
            Toast.makeText(this, "Pokémon guardado", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
        }
    }


}