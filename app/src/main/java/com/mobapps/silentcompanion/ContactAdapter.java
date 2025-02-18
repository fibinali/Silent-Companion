package com.mobapps.silentcompanion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private Context context;
    private List<Contact> contactList;
    private DatabaseHelper databaseHelper;

    public ContactAdapter(Context context, List<Contact> contactList) {
        this.context = context;
        this.contactList = contactList;
        databaseHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.numberTextView.setText(contact.getNumber());

        holder.deleteButton.setOnClickListener(v -> {
            boolean isDeleted = databaseHelper.deleteContact(contact.getId());
            if (isDeleted) {
                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show();
                contactList.remove(position);
                notifyItemRemoved(position);
            } else {
                Toast.makeText(context, "Error deleting contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, numberTextView;
        ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contact_name);
            numberTextView = itemView.findViewById(R.id.contact_number);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }

    // Contact model class
    public static class Contact {
        private int id;
        private String name;
        private String number;

        public Contact(int id, String name, String number) {
            this.id = id;
            this.name = name;
            this.number = number;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }
    }
}
