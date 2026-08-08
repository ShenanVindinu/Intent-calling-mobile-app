package uk.ac.wlv.shenanapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Uri photoUri;
    private ImageView imageView;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    imageView.setImageURI(photoUri);
                    imageView.setVisibility(ImageView.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    imageView.setImageURI(selectedImageUri);
                    imageView.setVisibility(ImageView.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openContactPicker();
                }
            });

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    if (contactUri != null) {
                        String phoneNumber = getPhoneNumberFromContact(contactUri);
                        if (phoneNumber != null) {
                            placeCall(phoneNumber);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> shareImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    try {
                        String mimeType = getContentResolver().getType(selectedImageUri);
                        if (mimeType == null) mimeType = "image/jpeg";

                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, "shared_image_" + System.currentTimeMillis() + ".jpg");
                        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                        Uri mediaUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                        try (java.io.InputStream in = getContentResolver().openInputStream(selectedImageUri);
                             java.io.OutputStream out = getContentResolver().openOutputStream(mediaUri)) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        }

                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this awesome app!");
                        sendIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                        sendIntent.setType(mimeType);
                        sendIntent.setClipData(android.content.ClipData.newUri(getContentResolver(), "", mediaUri));
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Intent chooserIntent = Intent.createChooser(sendIntent, "Share via");
                        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(chooserIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    private String getPhoneNumberFromContact(Uri phoneUri) {
        String phoneNumber = null;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = getContentResolver().query(phoneUri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(0);
            }
            cursor.close();
        }
        return phoneNumber;
    }

    private void openContactPicker() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(contactPickerIntent);
    }

    private void placeCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.imageView);

        Button btnShareTextImage = findViewById(R.id.buttonShareTextImage);
        btnShareTextImage.setOnClickListener(v -> {
            String[] options = {"Share Text", "Share Image"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Choose share type")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            android.widget.EditText input = new android.widget.EditText(this);
                            input.setHint("Enter text to share");
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("Share Text")
                                    .setView(input)
                                    .setPositiveButton("Share", (d, w) -> {
                                        String textToShare = input.getText().toString();
                                        if (!textToShare.isEmpty()) {
                                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                            sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
                                            sendIntent.setType("text/plain");
                                            startActivity(Intent.createChooser(sendIntent, "Share via"));
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                            galleryIntent.setType("image/*");
                            shareImageLauncher.launch(galleryIntent);
                        }
                    })
                    .show();
        });

        Button btnTakePhoto = findViewById(R.id.button2);
        btnTakePhoto.setOnClickListener(v -> {
            File photoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(cameraIntent);
        });

        Button btnDialPad = findViewById(R.id.button3);
        btnDialPad.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            } else {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
            }
        });

        Button btnGallery = findViewById(R.id.button4);
        btnGallery.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            galleryLauncher.launch(galleryIntent);
        });
    }
}