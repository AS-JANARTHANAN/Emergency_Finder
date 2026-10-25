package com.emergencyroutefinder.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.model.AuthResponse;
import com.emergencyroutefinder.ui.user.UserDashboardActivity;
import com.emergencyroutefinder.ui.worker.WorkerDashboardActivity;
import com.emergencyroutefinder.util.SessionManager;
import com.emergencyroutefinder.network.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.util.DotPasswordTransformationMethod;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login Activity — role-based login screen.
 * Demonstrates: GUI, Network calls on background thread, Exception Handling
 */
public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private TextInputLayout tilPassword;
    private RadioGroup rgRole;
    private Button btnLogin;
    private TextView tvRegister, tvError;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        tilPassword = findViewById(R.id.til_password);
        etPassword.setTransformationMethod(DotPasswordTransformationMethod.getInstance());
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
        rgRole      = findViewById(R.id.rg_role);
        btnLogin    = findViewById(R.id.btn_login);
        tvRegister  = findViewById(R.id.tv_register);
        tvError     = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> openRegistration());
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Client-side validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        String selectedRole = (rgRole.getCheckedRadioButtonId() == R.id.rb_worker) ? "WORKER" : "USER";

        setLoading(true);
        tvError.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("role", selectedRole);

        FirebaseManager.getInstance().login(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseManager.getInstance().getAuth().getCurrentUser();
                        if (user != null) {
                            // Get role from Firestore
                            FirebaseManager.getInstance().getUserRecord(user.getUid())
                                    .addOnCompleteListener(dbTask -> {
                                        setLoading(false);
                                        if (dbTask.isSuccessful() && dbTask.getResult() != null && dbTask.getResult().exists()) {
                                            DocumentSnapshot doc = dbTask.getResult();
                                            String dbRole = doc.getString("role");
                                            if (!selectedRole.equalsIgnoreCase(dbRole)) {
                                                FirebaseManager.getInstance().getAuth().signOut();
                                                showError("Role mismatch: Account is registered as " + dbRole + ". Please select the correct role.");
                                                return;
                                            }
                                            SessionManager.getInstance(LoginActivity.this)
                                                    .saveSession(user.getUid(), dbRole,
                                                            1, doc.getString("name"), user.getEmail());
                                            navigateToDashboard(dbRole);
                                        } else {
                                            FirebaseManager.getInstance().getAuth().signOut();
                                            showError("User record not found in database.");
                                        }
                                    });
                        }
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        showError(msg);
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("WORKER".equals(role)) {
            intent = new Intent(this, WorkerDashboardActivity.class);
        } else {
            intent = new Intent(this, UserDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openRegistration() {
        int selectedId = rgRole.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_worker) {
            startActivity(new Intent(this, WorkerRegisterActivity.class));
        } else {
            startActivity(new Intent(this, UserRegisterActivity.class));
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : "Sign In");
    }
}
