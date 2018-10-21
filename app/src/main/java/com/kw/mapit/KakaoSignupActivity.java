package com.kw.mapit;

/**
 * Created by ywhy1 on 2018-03-25.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.kakao.auth.ErrorCode;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.helper.log.Logger;

public class KakaoSignupActivity extends Activity {
    String userName;
    String userId;
    String userEmail;


    //savedInstanceState : 기존 세션 정보가 저장된 객체
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestMe();
    }

    //사용자의 정보 가져오기
    protected void requestMe() {
        UserManagement.getInstance().requestMe(new MeResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                String message = "failed to get user info. msg=" + errorResult;
                Logger.d(message);
                ErrorCode result = ErrorCode.valueOf(errorResult.getErrorCode());
                if (result == ErrorCode.CLIENT_ERROR_CODE) {
                    finish();
                } else {
                    redirectLoginActivity();
                }
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                redirectLoginActivity();
            }

            //카카오톡 회원이 아닐 때
            @Override
            public void onNotSignedUp() {
                //showSignup()
            }

            //로그인 성공 시 사용자 정보 받아오기
            @Override
            public void onSuccess(UserProfile userProfile) {
                Log.e("superdroid","UserProfile : " + userProfile);


                Log.e("test","로그인 성공!");

                userName = userProfile.getNickname();
                //userId = String.valueOf(userProfile.getId());
                userEmail = userProfile.getEmail();

                redirectInputDataActivity();
            }
        });
    }

    //InputDataActivity 이동
    private void redirectInputDataActivity() {
        Intent userProfileIntent = new Intent(this, InputDataActivity.class);
        userProfileIntent.putExtra("USER_ID", userId);
        userProfileIntent.putExtra("USER_NAME", userName);
        userProfileIntent.putExtra("USER_EMAIL", userEmail);
        startActivity(userProfileIntent);
        finish();
    }

    //LoginActivity 이동
    protected void redirectLoginActivity() {
        final Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

}
