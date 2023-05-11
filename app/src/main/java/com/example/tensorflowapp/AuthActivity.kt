package com.example.tensorflowapp

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.tensorflowapp.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private var status = true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        checkLogInState(applicationContext)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.findNavController()
        binding.btnChangeLog.isChecked = status
        binding.btnChangeLog.setOnClickListener {
            if (status) {
                navController.navigate(R.id.action_loginFragment_to_registrationFragment, )
                status = false
            } else {
                navController.navigate(R.id.action_registrationFragment_to_loginFragment)
                status = true
            }
        }
    }

    fun checkLogInState(context: Context){
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser !== null) {
            var intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        finish()
    }
}