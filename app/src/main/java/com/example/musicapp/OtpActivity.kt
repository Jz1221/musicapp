package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.musicapp.databinding.ActivityOtpBinding
import com.google.firebase.auth.FirebaseAuth
import papaya.`in`.sendmail.SendMail
import kotlin.random.Random

class OtpActivity : AppCompatActivity() {

    lateinit var binding: ActivityOtpBinding
    lateinit var auth: FirebaseAuth
    var email: String = ""
    var pass: String = ""
    var generatedOtp: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get email & password passed from previous activity
        email = intent.getStringExtra("email") ?: ""
        pass = intent.getStringExtra("pass") ?: ""
        auth = FirebaseAuth.getInstance()

        // Show email on screen
        binding.showEmail.text = email

        // Send the initial OTP
        sendOtp()

        // Resend OTP click
        binding.tvResend.setOnClickListener {
            Log.d("OTP_DEBUG", "Resend OTP clicked")
            sendOtp()
        }

        // Auto focus handling for OTP fields
        binding.otp1.doOnTextChanged { _, _, _, _ ->
            if (binding.otp1.text!!.isNotEmpty()) binding.otp2.requestFocus()
        }
        binding.otp2.doOnTextChanged { _, _, _, _ ->
            if (binding.otp2.text!!.isNotEmpty()) binding.otp3.requestFocus() else binding.otp1.requestFocus()
        }
        binding.otp3.doOnTextChanged { _, _, _, _ ->
            if (binding.otp3.text!!.isNotEmpty()) binding.otp4.requestFocus() else binding.otp2.requestFocus()
        }
        binding.otp4.doOnTextChanged { _, _, _, _ ->
            if (binding.otp4.text!!.isNotEmpty()) binding.otp5.requestFocus() else binding.otp3.requestFocus()
        }
        binding.otp5.doOnTextChanged { _, _, _, _ ->
            if (binding.otp5.text!!.isNotEmpty()) binding.otp6.requestFocus() else binding.otp4.requestFocus()
        }
        binding.otp6.doOnTextChanged { _, _, _, _ ->
            if (binding.otp6.text!!.isEmpty()) binding.otp5.requestFocus()
        }

        // Verify OTP
        binding.button.setOnClickListener {
            val enteredOtp = listOf(
                binding.otp1.text.toString(),
                binding.otp2.text.toString(),
                binding.otp3.text.toString(),
                binding.otp4.text.toString(),
                binding.otp5.text.toString(),
                binding.otp6.text.toString()
            ).joinToString("")

            if (enteredOtp.length != 6) {
                Toast.makeText(this, "Enter complete OTP", Toast.LENGTH_SHORT).show()
            } else if (enteredOtp != generatedOtp.toString()) {
                Toast.makeText(this, "Wrong OTP", Toast.LENGTH_SHORT).show()
            } else {
                // Correct OTP - proceed to MainActivity

                Toast.makeText(this, "OTP verification successful, welcome to our app.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
                finish()
            }
        }
    }

    // Function to generate and send OTP to user's email
    private fun sendOtp() {
        // Generate new OTP
        generatedOtp = Random.nextInt(100000, 999999)
        Log.d("OTP_DEBUG", "Sending OTP $generatedOtp to $email")

        // Disable Resend temporarily for 30 seconds
        binding.tvResend.isEnabled = true
        binding.tvResend.alpha = 0.5f
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvResend.isEnabled = true
            binding.tvResend.alpha = 1f
        }, 30000)

        // Send the OTP email
        try {
            val mail = SendMail(
                "jzheng0512@gmail.com",         // sender email
                "jnxrckfgmfwdlvny",             // app password
                email,                                      // recipient email
                "Music App OTP",                   // subject
                "Your OTP is -> $generatedOtp"   // message
            )
            mail.execute()
            Toast.makeText(this, "OTP sent to your email", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
