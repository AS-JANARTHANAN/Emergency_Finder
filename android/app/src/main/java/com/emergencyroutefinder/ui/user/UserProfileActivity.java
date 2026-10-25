package com.emergencyroutefinder.ui.user;
import android.os.Bundle;
import android.widget.TextView;
import com.emergencyroutefinder.ui.BaseActivity;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.util.SessionManager;

public class UserProfileActivity extends BaseActivity {
    @Override 
    protected void onCreate(Bundle s) { 
        super.onCreate(s); 
        setContentView(R.layout.activity_user_profile); 
        
        TextView tvName = findViewById(R.id.tv_name);
        TextView tvEmail = findViewById(R.id.tv_email);
        TextView tvMobile = findViewById(R.id.tv_mobile);
        
        SessionManager session = SessionManager.getInstance(this);
        if(tvName != null) tvName.setText(session.getFullName());
        if(tvEmail != null) tvEmail.setText(session.getEmail());
        if(tvMobile != null) tvMobile.setText("Not Provided");
    }
}
