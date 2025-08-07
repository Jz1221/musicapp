package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Sign Up Link
        val signUpBtn = findViewById<Button>(R.id.btnSignUp)

        signUpBtn.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)//Link to Sign Up Page
            startActivity(intent)
        }

        //Login Link
        val loginBtn = findViewById<Button>(R.id.btnLogin)

        loginBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) //Link to Login Page
            startActivity(intent)
        }
    }
}