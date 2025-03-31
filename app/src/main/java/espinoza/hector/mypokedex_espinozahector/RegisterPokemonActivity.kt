package espinoza.hector.mypokedex_espinozahector

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class RegisterPokemonActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_GET = 1
    private val CLOUD_NAME = "dtejuoctt"
    private val UPLOAD_PRESET = "pokemon-upload"
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_pokemon)

        val name: EditText = findViewById(R.id.pokemonName)
        val pNumber: EditText = findViewById(R.id.pokemonNumber)
        val select: Button = findViewById(R.id.selectImage)
        val save: Button = findViewById(R.id.savePokemon)

        select.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }

        save.setOnClickListener {
            val pokemonName = name.text.toString().trim()
            val pokemonNumber = pNumber.text.toString().trim()

            if (pokemonName.isEmpty() || pokemonNumber.isEmpty() || imageUri == null) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                uploadImageToCloudinary(pokemonName, pokemonNumber)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageUri?.let { changeImage(it) }
        }
    }

    private fun changeImage(uri: Uri) {
        val thumbnail: ImageView = findViewById(R.id.thumbnail)
        thumbnail.setImageURI(uri)
    }

    private fun uploadImageToCloudinary(name: String, number: String) {
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        val filePath = getRealPathFromURI(imageUri!!)
        val file = File(filePath)
        val url = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

        Thread {
            try {
                val boundary = "Boundary-" + System.currentTimeMillis()
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                outputStream.writeBytes("$UPLOAD_PRESET\r\n")

                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
                outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")

                val inputStream = FileInputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.writeBytes("\r\n--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val imageUrl = jsonObject.getString("secure_url")

                    runOnUiThread {
                        savePokemonToDatabase(name, number, imageUrl)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun savePokemonToDatabase(name: String, number: String, imageUrl: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference.child("Pokemns").child(number)
        val pokemon = Pokemon(name, number.toInt(), imageUrl)

        databaseRef.setValue(pokemon).addOnSuccessListener {
            Toast.makeText(this, "Pokémon guardado", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result ?: uri.path!!
    }
}
