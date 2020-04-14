package tkuo.sportsmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class CreatePersonalMatchActivity extends AppCompatActivity {
    private Button createButton;
    private TextView textView;
    private EditText location, date, start, end, numberOfPlayer;
    private ArrayAdapter<String> adapter;
    private Spinner spinner;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, PersonalMatchRef;
    private StorageReference personalMatchReference;
    private String currentUserID;
    static private long personal_id = 0;


    private static final String[] gameType = {"1 on 1", "3 on 3", "5 on 5"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_personal_match);//create the view of create personal match
        mAuth = FirebaseAuth.getInstance();//get firebase instance

        // Connects to current user
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("User").child(currentUserID);
        PersonalMatchRef = FirebaseDatabase.getInstance().getReference().child("PersonalMatch");

        createButton = (Button) findViewById(R.id.create);
        textView = (TextView) findViewById(R.id.title);
        location = (EditText) findViewById(R.id.location);
        date = (EditText) findViewById(R.id.date);
        start = (EditText) findViewById(R.id.start_time);
        end = (EditText) findViewById(R.id.end_time);
        numberOfPlayer = (EditText) findViewById(R.id.player_number);

        loadingBar = new ProgressDialog(this);

        spinner = (Spinner)findViewById(R.id.game_type);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, gameType);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePersonalMatchInformation();
            }
        });

    }

    private void savePersonalMatchInformation() {
        String location_s = location.getText().toString();
        String date_s = date.getText().toString();
        String start_from = start.getText().toString();
        String end_to = end.getText().toString();
        String number = numberOfPlayer.getText().toString();

        if(TextUtils.isEmpty(location_s)){
            Toast.makeText(this, "Please write the location of this game...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(date_s)){
            Toast.makeText(this, "Please write the date of this game...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(start_from)){
            Toast.makeText(this, "Please write the start time...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(end_to)){
            Toast.makeText(this, "Please write the end time...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(number)){
            Toast.makeText(this, "Please write the number of players...", Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Creating New Personal Match");
            loadingBar.setMessage("Please wait, while we are creating new personal match...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            HashMap matchInfo = new HashMap();
            matchInfo.put("host_id", currentUserID);
            matchInfo.put("location", location_s);
            matchInfo.put("date", date_s);
            matchInfo.put("start_time", start_from);
            matchInfo.put("end_time", end_to);
            matchInfo.put("num_of_players", number);

            PersonalMatchRef.child(String.valueOf(personal_id)).updateChildren(matchInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()) {
                        // startActivity(new Intent(getApplicationContext(),MainActivity.class));    // This works the same as senUserToMainActivity()
                        sendUserToMainActivity();
                        Toast.makeText(CreatePersonalMatchActivity.this, "Your personal match is created successfully.", Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                        personal_id++;
                    }
                    else {
                        String message = task.getException().getMessage();
                        Toast.makeText(CreatePersonalMatchActivity.this, "Error occurred:" + message, Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    // Switch to Main Activity and clear any other activities on top of it
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(CreatePersonalMatchActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Do this to prevent user from going back to Register activity unless clicking logout
        startActivity(mainIntent);
        finish();
    }

}
