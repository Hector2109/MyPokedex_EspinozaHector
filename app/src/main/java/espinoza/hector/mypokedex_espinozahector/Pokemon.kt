package espinoza.hector.mypokedex_espinozahector

import android.net.Uri

data class Pokemon(
    val name: String = "",
    val number: Int = 0,
    val uri: String = ""
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", 0, "")
}
