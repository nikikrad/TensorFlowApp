package com.example.tensorflowapp.presentation.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginFragment: Fragment() {

    private lateinit var binding: FragmentLoginBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        checkLoggedInState()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnLogOut.setOnClickListener {
            auth.signOut()
            checkLoggedInState()
        }
        binding.btnLogIn.setOnClickListener {
            logIn()
        }

    }

    private fun logIn() {
        CoroutineScope(Dispatchers.Main).launch {
            if (binding.etLogin.text !== null && binding.etPassword.text?.length!! >= 6) {

                auth.signInWithEmailAndPassword(
                    binding.etLogin.text.toString(),
                    binding.etPassword.text.toString()
                ).addOnFailureListener {
                    binding.tvErrorLabel.text = "Неправильный логин или пароль"
                    binding.tvErrorLabel.isVisible = true
                }.addOnSuccessListener {
                    checkLoggedInState()
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                }
            } else {
                binding.tvErrorLabel.text = "Неправильный ввод данных!"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkLoggedInState() {
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
        if (auth.currentUser !== null) {
            binding.tvSignInLabel.text = "Вы онлайн!"
            binding.tvEmail.text = auth.currentUser?.email.toString()
            binding.etLogin.isVisible = false
            binding.etPassword.isVisible = false
            binding.btnLogIn.isVisible = false
            binding.tvErrorLabel.isVisible = false
            binding.btnLogOut.isVisible = true
        } else {
            binding.tvSignInLabel.text = "Войдите в аккаунт"
            binding.etLogin.isVisible = true
            binding.etPassword.isVisible = true
            binding.btnLogIn.isVisible = true
            binding.tvEmail.isVisible = false
            binding.btnLogOut.isVisible = false
        }
    }
}