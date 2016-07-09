package kr.edcan.cumchuck.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import kr.edcan.cumchuck.R;
import kr.edcan.cumchuck.model.FacebookUser;
import kr.edcan.cumchuck.model.User;
import kr.edcan.cumchuck.utils.CumChuckHelper;
import kr.edcan.cumchuck.utils.CumChuckNetworkHelper;
import kr.edcan.cumchuck.utils.DataManager;
import kr.edcan.cumchuck.utils.NetworkInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    LoginButton fbLogin;
    CallbackManager manager;
    NetworkInterface service;
    Call<FacebookUser> loginByFacebook;
    DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        initialize();
        validateUserToken();
    }

    private void validateUserToken() {
        Pair<Boolean, User> userPair = dataManager.getActiveUser();
        if (userPair.first == false) {
            setFacebook();
            setTwitter();
        } else {
            // validate
            Toast.makeText(this, userPair.second.getName()+ " 님 안녕하세요!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initialize() {
        AppEventsLogger.activateApp(this);
        manager = CallbackManager.Factory.create();
        dataManager = new DataManager();
        fbLogin = (LoginButton) findViewById(R.id.auth_facebookbutton);
        dataManager.initializeManager(getApplicationContext());

        service = CumChuckNetworkHelper.getNetworkInstance();
    }

    private void setFacebook() {
        LoginManager.getInstance().registerCallback(manager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                new LoadInfo().execute(loginResult.getAccessToken().getToken());
                dataManager.saveUserToken(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(AuthActivity.this, "로그인 인증 중에 문제가 발생했습니다.\n서비스 관리자에게 문의해주세요.", Toast.LENGTH_SHORT).show();
                CumChuckHelper.Log(AuthActivity.this.getClass().getName(), error.getMessage());
            }
        });
    }

    private void setTwitter() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        manager.onActivityResult(requestCode, resultCode, data);
    }

    class LoadInfo extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            loginByFacebook = service.loginByFacebook(strings[0]);
            loginByFacebook.enqueue(new Callback<FacebookUser>() {
                @Override
                public void onResponse(Call<FacebookUser> call, Response<FacebookUser> response) {
                    switch (response.code()) {
                        case 200:
                            FacebookUser facebookUser = response.body();
                            dataManager.saveUserInfo(facebookUser);
                            startActivity(new Intent(AuthActivity.this, MainActivity.class));
                            break;
                        case 401:
                            CumChuckHelper.Log(AuthActivity.this.getLocalClassName(), "Unauthorized");
                            Toast.makeText(AuthActivity.this, "인증 과정에서 문제가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }

                @Override
                public void onFailure(Call<FacebookUser> call, Throwable t) {
                    CumChuckHelper.Log(AuthActivity.this.getLocalClassName(), t.getMessage());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}