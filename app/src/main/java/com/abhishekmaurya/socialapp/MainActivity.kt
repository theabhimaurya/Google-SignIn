package com.abhishekmaurya.socialapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.provider.SyncStateContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abhishekmaurya.socialapp.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object{
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_LOCATION = 123
        private const val PERMISSION_ID = 42

    }
    lateinit var mGoogleMap: GoogleMap
    var mCurrLocationMarker: Marker? = null
    var mapFrag: SupportMapFragment? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var bottomCount = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    val ANIMATION_IN: Int? = null
    val ANIMATION_OUT: Int? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient : GoogleSignInClient
    private var RC_SIGN_IN = 75

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClasses()
        initViews()
        initData()
    }

    private fun initClasses() {
        auth = Firebase.auth

        supportActionBar?.title = "Map Location Activity"

        mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFrag?.getMapAsync(this)

    }

    private fun initViews() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.button.setOnClickListener {
            signIn()
        }
    }
    private fun initData() {
        binding.logoutBtn.setOnClickListener {
            auth.signOut()
            finish();
            overridePendingTransition(0, 0);
            startActivity(getIntent())
            overridePendingTransition(0, 0);
        }

        requestPermissionToAccessLocation()
    }
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                Toast.makeText(this, "Login SuccessFully", Toast.LENGTH_SHORT).show()
//                Toast.makeText(this, "${account.email}", Toast.LENGTH_SHORT).show()
                Glide
                    .with(this)
                    .load(account.photoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.common_full_open_on_phone)
                    .into(binding.imageView)
                binding.textView.text = "${account.displayName} \n${account.email}"
//                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

//    private fun firebaseAuthWithGoogle(idToken: String) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    // Sign in success, update UI with the signed-in user's information
//                    Log.d(TAG, "signInWithCredential:success")
//
////                    Toast.makeText(this, "Success Full Login", Toast.LENGTH_SHORT).show()
//                    val user = auth.currentUser
////                    updateUI(user)
//                } else {
//                    // If sign in fails, display a message to the user.
//                    Log.w(TAG, "signInWithCredential:failure", task.exception)
////                    updateUI(null)
//                }
//            }
//    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
//        updateUI(currentUser)
    }
    private fun updateUI(currentUser: FirebaseUser?) {
        currentUser?.phoneNumber
    }

    private fun requestPermissionToAccessLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION
                )
            }
        } else {
            getLocation()
        }
    }
    private fun getLocation() {
//        Log(TAG, "initLocation: called")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        latitude = location.latitude
                        longitude = location.longitude
                        getHomeNearByData(latitude, longitude)
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                this.overridePendingTransition(
                    ANIMATION_IN!!,
                    ANIMATION_OUT!!
                )
            }
        } else {
            requestPermissions()
        }
    }

    private fun getHomeNearByData(latitude: Double, longitude: Double) {

        Toast.makeText(this, "latitude: $latitude,  longitude: $longitude", Toast.LENGTH_SHORT).show()

        val latLng = LatLng(latitude, longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current Position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))

        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions)

        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.0F))
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation

//            UtilsManager.showLog(TAG,
//                "onLocationResult: this is location latitude 222 = ${mLastLocation.latitude}"
//            )

            latitude = mLastLocation.latitude
            longitude = mLastLocation.longitude
            getHomeNearByData(latitude, longitude)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest.create().apply {
            interval = 100
            fastestInterval = 50
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 100
        }

        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun moveToSettings() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permissions Required")
        builder.setMessage("It is necessary to know where you are to see what parishes are close to you. Please allow location permissions in the settings")
        builder.setPositiveButton(
            "Grant"
        ) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog: DialogInterface?, _: Int -> dialog!!.dismiss() }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this.packageName, null)
        intent.data = uri
        startActivity(intent)
//        this.overridePendingTransition(Constants.ANIMATION_IN, Constants.ANIMATION_OUT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION || requestCode == PERMISSION_ID) {
            if (permissions.isNotEmpty() && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    progressManager.dismissDialog()
                    getLocation()
                } else {
                    moveToSettings()
                }
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mGoogleMap = p0
        mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
    }

}
