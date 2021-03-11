package com.sizeadviser.sizeadviser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser

import com.sizeadviser.sizeadviser.databinding.ActivityEmailPasswordSignInBinding

class EmailPasswordSignInActivity : BaseActivity(), View.OnClickListener {

    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
    // [END declare_auth]

    private lateinit var binding: ActivityEmailPasswordSignInBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailPasswordSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setProgressBar(binding.progressBar)

        // Buttons
        binding.emailSignInButton.setOnClickListener(this)
        binding.signOutButton.setOnClickListener(this)
        binding.verifyEmailButton.setOnClickListener(this)
        binding.reloadButton.setOnClickListener(this)
        binding.forgotPassword.setOnClickListener(this)
        binding.NoAccount.setOnClickListener(this)

        binding.fieldEmail.setText(intent.getStringExtra("userEmail"))

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // [END initialize_auth]
    }

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
    // [END on_start_check_user]

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm()) {
            return
        }

        hideStatusString()
        showProgressBar()

        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                    // [START_EXCLUDE]
                    checkForMultiFactorFailure(task.exception!!)
                    // [END_EXCLUDE]
                }

                // [START_EXCLUDE]
                if (!task.isSuccessful) {
                    showStatusString()
                    binding.status.setText(R.string.invalid_creds)
                }
                hideProgressBar()
                // [END_EXCLUDE]
            }
        // [END sign_in_with_email]
    }

    private fun hideStatusString() {
        binding.status.setTextColor(Color.TRANSPARENT)
    }

    private fun showStatusString() {
        binding.status.setTextColor(Color.RED)
    }

    private fun signOut() {
        auth.signOut()
        updateUI(null)
    }

    private fun resetPassword() {
        auth.sendPasswordResetEmail(binding.fieldEmail.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, "Reset email sent.",
                        Toast.LENGTH_LONG).show()
                    showStatusString()
                    binding.status.text = "Reset your password and try again"
                }
            }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = binding.fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.fieldEmail.error = "Required."
            valid = false
        } else {
            binding.fieldEmail.error = null
        }

        val password = binding.fieldPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.fieldPassword.error = "Required."
            valid = false
        } else {
            binding.fieldPassword.error = null
        }

        return valid
    }

    private fun updateUI(user: FirebaseUser?) {
        hideProgressBar()
        if (user != null) {
            if (user.isEmailVerified) {
                binding.verifyEmailButton.visibility = View.GONE
                if (user.displayName != null) {
                    val intent = Intent(this@EmailPasswordSignInActivity, CalibrationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else {
                    val intent = Intent(this@EmailPasswordSignInActivity, EnterUsernameActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                showStatusString()
                binding.status.text = "Please verify your e-mail first"
            }
        }
    }

    private fun createAccountScreen() {
        val intent = Intent(this@EmailPasswordSignInActivity, EmailPasswordActivity::class.java)
        val b = Bundle()
        b.putString("userEmail", binding.fieldEmail.text.toString())
        intent.putExtras(b)
        startActivity(intent)
        finish()
    }

    private fun checkForMultiFactorFailure(e: Exception) {
        // Multi-factor authentication with SMS is currently only available for
        // Google Cloud Identity Platform projects. For more information:
        // https://cloud.google.com/identity-platform/docs/android/mfa
        if (e is FirebaseAuthMultiFactorException) {
            Log.w(TAG, "multiFactorFailure", e)
            val intent = Intent()
            val resolver = e.resolver
            intent.putExtra("EXTRA_MFA_RESOLVER", resolver)
            setResult(MultiFactorActivity.RESULT_NEEDS_MFA_SIGN_IN, intent)
            finish()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.emailSignInButton -> signIn(binding.fieldEmail.text.toString(), binding.fieldPassword.text.toString())
            R.id.forgotPassword -> resetPassword()
            R.id.NoAccount -> createAccountScreen()
        }
    }

    companion object {
        private const val TAG = "EmailPasswordSignIn"
        private const val RC_MULTI_FACTOR = 9005
    }
}