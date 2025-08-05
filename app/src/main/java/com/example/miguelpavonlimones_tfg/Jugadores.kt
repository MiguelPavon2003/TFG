package com.example.miguelpavonlimones_tfg
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Jugadores : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_jugadores)
        bottomNavigation = findViewById(R.id.bottomNavigation)


        bottomNavigation.selectedItemId = R.id.nav_jugadores
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
                    true
                }
                R.id.nav_configuracion -> {
                    if (this.javaClass != Configuracion::class.java) {
                        startActivity(Intent(this, Configuracion::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}