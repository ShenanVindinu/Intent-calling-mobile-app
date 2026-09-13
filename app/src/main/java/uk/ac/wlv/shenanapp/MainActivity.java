package uk.ac.wlv.shenanapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


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

public class MainActivity extends AppCompatActivity {

    private Uri photoUri;
    private Uri shareImageUri;
    private String selectedPhoneNumber;
    private ImageView imageView;
    private ImageView shareImagePreview;
    private TextView selectedContactView;
    private Button callSelectedContactButton;

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

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    if (contactUri != null) {
                        showSelectedContact(contactUri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> shareImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    shareImageUri = result.getData().getData();
                    if (shareImageUri != null && shareImagePreview != null) {
                        shareImagePreview.setImageURI(shareImageUri);
                        shareImagePreview.setVisibility(View.VISIBLE);
                    }
                }
            });

    private void showSelectedContact(Uri phoneUri) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        Cursor cursor = getContentResolver().query(phoneUri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                selectedPhoneNumber = cursor.getString(1);
                selectedContactView.setText(getString(R.string.selected_contact, name, selectedPhoneNumber));
                callSelectedContactButton.setVisibility(View.VISIBLE);
            }
            cursor.close();
        }
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
        selectedContactView = findViewById(R.id.selectedContact);
        callSelectedContactButton = findViewById(R.id.buttonCallSelectedContact);

        Button btnShareTextImage = findViewById(R.id.buttonShareTextImage);
        btnShareTextImage.setOnClickListener(v -> showShareDialog());

        Button btnTakePhoto = findViewById(R.id.button2);
        btnTakePhoto.setOnClickListener(v -> {
            File photoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(cameraIntent);
        });

        Button btnDialPad = findViewById(R.id.button3);
        btnDialPad.setOnClickListener(v -> openContactPicker());

        callSelectedContactButton.setOnClickListener(v -> {
            if (selectedPhoneNumber != null) {
                placeCall(selectedPhoneNumber);
            }
        });

        Button btnGallery = findViewById(R.id.button4);
        btnGallery.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            galleryLauncher.launch(galleryIntent);
        });
    }

    private void showShareDialog() {
        shareImageUri = null;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);

        EditText textInput = new EditText(this);
        textInput.setHint(R.string.share_text_hint);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.addView(textInput);

        Button attachImageButton = new Button(this);
        attachImageButton.setText(R.string.attach_image);
        content.addView(attachImageButton);

        shareImagePreview = new ImageView(this);
        shareImagePreview.setAdjustViewBounds(true);
        shareImagePreview.setMaxHeight((int) (180 * getResources().getDisplayMetrics().density));
        shareImagePreview.setVisibility(View.GONE);
        content.addView(shareImagePreview);

        attachImageButton.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            shareImageLauncher.launch(galleryIntent);
        });

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.share_text_image)
                .setView(content)
                .setPositiveButton(R.string.share, (dialog, which) -> shareContent(textInput.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void shareContent(String text) {
        if (text.trim().isEmpty() && shareImageUri == null) {
            return;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        if (shareImageUri != null) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, shareImageUri);
            sendIntent.setType("image/*");
            sendIntent.setClipData(android.content.ClipData.newUri(getContentResolver(), "image", shareImageUri));
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            sendIntent.setType("text/plain");
        }

        Intent chooser = Intent.createChooser(sendIntent, getString(R.string.share_via));
        Intent gmailIntent = new Intent(sendIntent).setPackage("com.google.android.gm");
        if (gmailIntent.resolveActivity(getPackageManager()) != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{gmailIntent});
        }
        startActivity(chooser);
    }
}
