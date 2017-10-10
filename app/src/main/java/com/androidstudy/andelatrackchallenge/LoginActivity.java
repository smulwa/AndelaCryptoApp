package com.androidstudy.andelatrackchallenge;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.androidstudy.andelatrackchallenge.models.User;
import com.androidstudy.andelatrackchallenge.utils.Settings;
import com.bumptech.glide.Glide;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.Box;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.mFacebookLogin)
    LoginButton mFacebookLogin;
    @BindView(R.id.mGoogleLogin)
    SignInButton mGoogleLogin;

    Settings settings;
    Box<User> userBox;

    private CallbackManager callbackManager;
    private static final String TAG = "Log";
    private static final int RC_SIGN_IN = 1;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);
        settings = new Settings(this.getApplicationContext());
        userBox = ((AndelaTrackChallenge) getApplicationContext()).getBoxStore().boxFor(User.class);
        callbackManager = CallbackManager.Factory.create();
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                //Load User Data
                loadUserData(loginResult);
            }

            @Override
            public void onCancel() {
                //Handle Cancel Here
            }

            @Override
            public void onError(FacebookException e) {
                //Handle Error Here
            }
        });

        mGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });
    }

    private void loadUserData(final LoginResult loginResult) {
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {

                        Log.i("Response ", response.toString());

                        // Application code
                        try {
                            String name = response.getJSONObject().getString("name");
                            String image_url = "http://graph.facebook.com/" + loginResult.getAccessToken().getUserId() +"/picture?type=large";

                            /**
                             * Save to ObjectBox ORM
                             * Set the Logged in status to true
                             * Navigate user to Main Activity
                             */
                            User user = new User();
                            user.name = name;
                            user.image_url = image_url;
                            userBox.put(user);

                            Log.d("Users", "Total Users : " + userBox.count());

                            settings.setLoggedInSharedPref(true);

                            Intent login = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(login);
                            finish();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,link");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            assert acct != null;

            String name = acct.getDisplayName();
            String image_url = String.valueOf(acct.getPhotoUrl());

            /**
             * Save to ObjectBox ORM
             * Set the Logged in status to true
             * Navigate user to Main Activity
             */
            User user = new User();
            user.name = name;
            user.image_url = image_url;
            userBox.put(user);

            Log.d("Users", "Total Users : " + userBox.count());

            settings.setLoggedInSharedPref(true);

            Intent login = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(login);
            finish();

        } else {
            // Signed out, show unauthenticated UI.

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

