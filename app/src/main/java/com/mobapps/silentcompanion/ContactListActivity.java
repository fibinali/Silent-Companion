package com.mobapps.silentcompanion;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactListActivity extends AppCompatActivity {

    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PERMISSION = 2;
    private RecyclerView recyclerView;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.recycleview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_PERMISSION);
        }

        Button addButton = findViewById(R.id.AddContact); // Replace with your button ID
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, REQUEST_CONTACT);
        });

        // Initially load contacts from the database
        loadContacts();
    }

    private void loadContacts() {
        List<ContactAdapter.Contact> contactList = databaseHelper.readAllContacts();
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
        } else {
            ContactAdapter contactAdapter = new ContactAdapter(this, contactList);
            recyclerView.setAdapter(contactAdapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                    String hasPhoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                    if (Integer.parseInt(hasPhoneNumber) > 0) {
                        Cursor phoneCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id},
                                null);

                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            insertContactToDatabase(name, phoneNumber);
                            phoneCursor.close();
                        }
                    }
                    cursor.close();
                }
            }
        }
    }

    private void insertContactToDatabase(String name, String number) {
        boolean isInserted = databaseHelper.insertContact(name, number);

        if (isInserted) {
            Toast.makeText(this, "Contact saved!", Toast.LENGTH_SHORT).show();
            // Refresh the contact list after insertion
            loadContacts();
        } else {
            Toast.makeText(this, "Error saving contact.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }
}
