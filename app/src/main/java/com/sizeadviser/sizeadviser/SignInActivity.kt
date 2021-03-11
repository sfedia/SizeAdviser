package com.sizeadviser.sizeadviser


import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.facebook.*
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.Logger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.sizeadviser.sizeadviser.databinding.ActivitySignInBinding
import com.sizeadviser.sizeadviser.EmailPasswordActivity
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.json.JSONObject


class SignInActivity : AppCompatActivity() {

    val RC_SIGN_IN: Int = 1
    var userEmail: String? = null
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    //val binding: ActivitySignInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
    private lateinit var firebaseAuth: FirebaseAuth
    val callbackManager = CallbackManager.Factory.create()

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, SignInActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_sign_in)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.paletteBlue)))

        val binding: ActivitySignInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        binding.useEmail.setOnClickListener {
            val intent = Intent(this@SignInActivity, EmailPasswordSignInActivity::class.java)
            val b = Bundle()
            b.putString("userEmail", binding.UserEmail.text.toString())
            intent.putExtras(b)
            startActivity(intent)
            finish()
        }

        binding.googleButton.setOnClickListener {
            signIn()
        }
        configureGoogleSignIn()
        firebaseAuth = FirebaseAuth.getInstance()


        login_button.setReadPermissions("email")


        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("FBLOGIN", loginResult.accessToken.token.toString())
                Log.d("FBLOGIN", loginResult.recentlyDeniedPermissions.toString())
                Log.d("FBLOGIN", loginResult.recentlyGrantedPermissions.toString())

                val credential = FacebookAuthProvider.getCredential(loginResult.accessToken.token)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this@SignInActivity) { task ->
                        if (task.isSuccessful) {
                            Log.d("FB", "signInWithCredential:success")
                            val user = firebaseAuth.currentUser
                            val intent = Intent(this@SignInActivity, CalibrationActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FB", "signInWithCredential:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

            }

            override fun onCancel() {
                Log.e("FBLOGIN_FAILD", "Cancel")
            }

            override fun onError(error: FacebookException) {
                Log.e("FBLOGIN_FAILD", "ERROR", error)
            }
        })

    }

    private fun dismissDialogLogin() {

    }

    override fun onStart() {
        super.onStart()
        // Google check
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val intent = Intent(this@SignInActivity, CalibrationActivity::class.java)
            startActivity(intent)
            finish()
        }
        // fb check
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        if (isLoggedIn && user != null) {
            val intent = Intent(this@SignInActivity, CalibrationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
        else if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(this@SignInActivity, CalibrationActivity::class.java)
                startActivity(intent)

            } else {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("862763273858-d8f1ts3qcvc9c15mkm0cm4hu3urn7ghp.apps.googleusercontent.com")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.fb -> {
                login_button.performClick()
            }
            R.id.gl -> {
                Log.d("onclick.journal", "'gl' click registered")
                signIn()
            }
        }
    }
}