package com.example.miguelpavonlimones_tfg
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Configuracion : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.nav_configuracion
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_partidos -> {
                    if (this.javaClass != pantalla2::class.java) {
                        startActivity(Intent(this, pantalla2::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_jugadores -> {
                    if (this.javaClass != Jugadores::class.java) {
                        startActivity(Intent(this, Jugadores::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_configuracion -> {
                    true
                }
                else -> false
            }
        }
    }
}