package tkuo.sportsmate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private EditText UserName, FullName;
    private RadioGroup Gender;
    private Button SaveInformationButton;
    private CircleImageView ProfileImage;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;
    private StorageReference UserProfileImageRef;
    private String currentUserID;
    final static int Gallery_Pick = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Connects to Firebase
        mAuth = FirebaseAuth.getInstance();

        // Connects to current user
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("User").child(currentUserID);
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        UserName = (EditText) findViewById(R.id.setup_username);
        FullName = (EditText) findViewById(R.id.setup_full_name);
        Gender = (RadioGroup) findViewById(R.id.setup_gender);
        SaveInformationButton = (Button) findViewById(R.id.setup_information_button);
        ProfileImage = (CircleImageView) findViewById(R.id.setup_profile_image);
        loadingBar = new ProgressDialog(this);


        SaveInformationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAccountSetupInformation();
            }
        });

        // Redirect user to mobile phone gallery and select a picture from it
        ProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });

        // Place the image in the circleImageView
        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("profileimage")) {
                    String image = dataSnapshot.child("profileimage").getValue().toString();
                    // Put the image to the profile picture in Setup Activity

                    Picasso.get()
                            .load(image)
                            .placeholder(R.drawable.profile)
                            .into(ProfileImage);



                }
                else {
                    Toast.makeText(SetupActivity.this, "Please select profile image first.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    // Let the user crop the image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Some conditions for the image
        if(requestCode == Gallery_Pick && resultCode == RESULT_OK && data!= null) {
            Uri ImageUri = data.getData();
            // Crop the image
            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        // Get the cropped image
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) { // Store the image into result
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK) {
                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we are updating your profile image...");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(true);


                final Uri resultUri = result.getUri();

                // Create file path to the Firebase storage
                final StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");

                // Put the file inside the profile image folder and store the image in it
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(SetupActivity.this, "Profile Image stored successfully...", Toast.LENGTH_SHORT).show();

                            // Get the image uri from Firebase storage
                            final String downloadUri = task.getResult().getStorage().getDownloadUrl().toString();

                            filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            final String downloadUri = uri.toString();
                                            UsersRef.child("profileimage").setValue(downloadUri).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(SetupActivity.this, "Image Stored", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                    else {
                                                        String message = task.getException().getMessage();
                                                        Toast.makeText(SetupActivity.this,"Error Occurred: " + message, Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
            else {
                Toast.makeText(this, "Error Occurred: Image can't be cropped. Try again.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }



    private void saveAccountSetupInformation() {

        // Get the selected radio button
        int radioButtonID = Gender.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) Gender.findViewById(radioButtonID);
        String gender = radioButton.getText().toString();

        // Get the typed username and full name
        String username = UserName.getText().toString();
        String fullName = FullName.getText().toString();

        if(TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please enter your username...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Please enter your full name...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Please Select your gender...", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Saving Information");
            loadingBar.setMessage("Please wait, while we are creating your new account...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            HashMap userMap = new HashMap();
            userMap.put("username", username);
            userMap.put("fullname", fullName);
            userMap.put("gender", gender);
            userMap.put("status", "Hey there, I am using Sportsmate to find a game to join!");
            userMap.put("team", "none");
            UsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()) {
                        // startActivity(new Intent(getApplicationContext(),MainActivity.class));    // This works the same as senUserToMainActivity()
                        sendUserToMainActivity();
                        Toast.makeText(SetupActivity.this, "Your account is created successfully.", Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                    }
                    else {
                        String message = task.getException().getMessage();
                        Toast.makeText(SetupActivity.this, "Error occurred:" + message, Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    // Switch to Main Activity and clear any other activities on top of it
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Do this to prevent user from going back to Register activity unless clicking logout
        startActivity(mainIntent);
        finish();
    }
}
