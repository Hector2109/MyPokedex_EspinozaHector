package espinoza.hector.mypokedex_espinozahector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private var pokemones = ArrayList<Pokemon>()
    private lateinit var adapter: AdaptadorPokemones
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val registerPokemon: Button = findViewById(R.id.btn_addPokemon)
        val listView: ListView = findViewById(R.id.pokemonList)

        // Inicializar adaptador y setearlo al ListView
        adapter = AdaptadorPokemones(this, pokemones)
        listView.adapter = adapter

        // Obtener referencia a Firebase
        database = FirebaseDatabase.getInstance().getReference("Pokemns")

        // Cargar Pokémon desde Firebase
        loadPokemonsFromFirebase()

        registerPokemon.setOnClickListener {
            val intent = Intent(this, RegisterPokemonActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPokemonsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pokemones.clear()

                // Iterar sobre los nodos hijos del nodo "Pokemons"
                for (pokemonSnapshot in snapshot.children) {
                    // Obtener los valores de cada Pokémon por separado
                    val nombre = pokemonSnapshot.child("name").getValue(String::class.java)
                    val numero = pokemonSnapshot.child("number").getValue(Int::class.java)
                    val uri = pokemonSnapshot.child("uri").getValue(String::class.java)

                    // Verificar si los datos son válidos antes de agregar el Pokémon a la lista
                    if (nombre != null && numero != null && uri != null) {
                        val pokemon = Pokemon(nombre, numero, uri)
                        pokemones.add(pokemon)
                        Log.d("Firebase", "Pokemon cargado: $pokemon")
                    } else {
                        Log.d("Firebase", "Datos incompletos para un Pokémon.")
                    }
                }

                // Verificar si la lista de pokemones está vacía
                if (pokemones.isEmpty()) {
                    Log.d("Firebase", "La lista de Pokémon está vacía después de cargar.")
                }

                // Notificar al adaptador que los datos han cambiado
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        })
    }

}

private class AdaptadorPokemones(contexto: Context, private var pokemones: ArrayList<Pokemon>) : BaseAdapter() {
    private var contexto: Context = contexto

    override fun getCount(): Int {
        return pokemones.size
    }

    override fun getItem(position: Int): Any {
        return pokemones[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return try {
            val pokemon = pokemones[position]
            val inflador = LayoutInflater.from(contexto)
            val vista = inflador.inflate(R.layout.pokemon_item, parent, false)

            val imagen = vista.findViewById<ImageView>(R.id.imagen_pokemon)
            val nombre = vista.findViewById<TextView>(R.id.nombre_pokemon)
            val numero = vista.findViewById<TextView>(R.id.numero_pokemon)

            contexto.let {
                Glide.with(it)
                    .load(pokemon.uri)
                    .into(imagen)
            }

            nombre.text = pokemon.name
            numero.text = "#${pokemon.number}"

            vista
        } catch (e: Exception) {
            Log.e("AdaptadorPokemones", "Error en getView", e)
            View(contexto) // Retornar una vista vacía en caso de error
        }
    }
}