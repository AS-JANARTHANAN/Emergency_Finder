package com.emergencyroutefinder.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.AuthResponse;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.ui.user.UserDashboardActivity;
import com.emergencyroutefinder.util.SessionManager;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.util.DotPasswordTransformationMethod;
import com.google.android.material.textfield.TextInputLayout;

/**
 * User Registration Activity.
 * Demonstrates: Form validation, async network, GUI
 */
public class UserRegisterActivity extends BaseActivity {

    private EditText etName, etEmail, etMobile, etPassword, etConfirm;
    private TextInputLayout tilPassword, tilConfirm;
    private CheckBox cbCrowdTraffic;
    private Button btnRegister;
    private TextView tvError;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        etName          = findViewById(R.id.et_full_name);
        etEmail         = findViewById(R.id.et_email);
        etMobile        = findViewById(R.id.et_mobile);
        etPassword      = findViewById(R.id.et_password);
        etConfirm       = findViewById(R.id.et_confirm_password);
        tilPassword     = findViewById(R.id.til_password);
        tilConfirm      = findViewById(R.id.til_confirm_password);

        etPassword.setTransformationMethod(DotPasswordTransformationMethod.getInstance());
        etConfirm.setTransformationMethod(DotPasswordTransformationMethod.getInstance());

        if (tilPassword != null) {
            tilPassword.setEndIconOnClickListener(v -> {
                int sel = etPassword.getSelectionEnd();
                if (etPassword.getTransformationMethod() instanceof DotPasswordTransformationMethod) {
                    etPassword.setTransformationMethod(null);
                } else {
                    etPassword.setTransformationMethod(DotPasswordTransformationMethod.getInstance());
                }
                if (sel >= 0) etPassword.setSelection(sel);
            });
        }
        if (tilConfirm != null) {
            tilConfirm.setEndIconOnClickListener(v -> {
                int sel = etConfirm.getSelectionEnd();
                if (etConfirm.getTransformationMethod() instanceof DotPasswordTransformationMethod) {
                    etConfirm.setTransformationMethod(null);
                } else {
                    etConfirm.setTransformationMethod(DotPasswordTransformationMethod.getInstance());
                }
                if (sel >= 0) etConfirm.setSelection(sel);
            });
        }

        cbCrowdTraffic  = findViewById(R.id.cb_crowd_traffic);
        btnRegister     = findViewById(R.id.btn_register);
        tvError         = findViewById(R.id.tv_error);
        progressBar     = findViewById(R.id.progress_bar);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tv_login).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String mobile   = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirm.getText().toString().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter a valid email address.");
            return;
        }
        if (mobile.length() < 10) {
            showError("Enter a valid mobile number.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        setLoading(true);
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", name);
        body.put("email", email);
        body.put("mobile", mobile);
        body.put("password", password);
        body.put("confirmPassword", confirm);
        body.put("allowCrowdTraffic", cbCrowdTraffic.isChecked());

        FirebaseManager.getInstance().register(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseManager.getInstance().getAuth().getCurrentUser();
                        if (user != null) {
                            FirebaseManager.getInstance().saveUserRecord(user.getUid(), "USER", name, email)
                                    .addOnCompleteListener(dbTask -> {
                                        setLoading(false);
                                        if (dbTask.isSuccessful()) {
                                            SessionManager.getInstance(UserRegisterActivity.this)
                                                    .saveSession(user.getUid(), "USER",
                                                            1, name, email);
                                            Intent intent = new Intent(UserRegisterActivity.this, UserDashboardActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            showError("Failed to save user data.");
                                        }
                                    });
                        }
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        showError(msg);
                    }
                });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
