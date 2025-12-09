package com.artifex.mupdf.viewer;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.viewer.gp.MuPDFLibrary;
import com.artifex.mupdf.viewer.gp.models.GPNote;

import java.util.List;

public class AllNotesAdapter extends RecyclerView.Adapter<AllNotesAdapter.NoteViewHolder> {

    private final List<GPNote> notes;
    private final DocumentActivity documentActivity;

    public AllNotesAdapter(List<GPNote> notes, DocumentActivity documentActivity) {
        this.notes = notes;
        this.documentActivity = documentActivity;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_all_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        GPNote note = notes.get(position);
        holder.tvNoteText.setText(note.getText());
        holder.btnEdit.setOnClickListener(v -> openEditDialog(v, note, position));
        holder.btnDelete.setOnClickListener(v -> confirmDelete(v, note, position));
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoteText;
        View btnEdit;
        View btnDelete;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteText = itemView.findViewById(R.id.tvNoteText);
            btnEdit = itemView.findViewById(R.id.edit_button);
            btnDelete = itemView.findViewById(R.id.delete_button);
        }
    }

    private void openEditDialog(View v, GPNote note, int position) {
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        RelativeLayout viewGroup = new RelativeLayout(v.getContext());
        View layout = inflater.inflate(R.layout.reader_note_popup, viewGroup);

        EditText edit = layout.findViewById(R.id.note_popup_edit_text);
        edit.setText(note.getText());
        edit.requestFocus();

        View cancel = layout.findViewById(R.id.note_popup_cancel);
        View save = layout.findViewById(R.id.note_popup_save);
        if (save instanceof TextView) {
            ((TextView) save).setText("Güncelle");
        }

        AlertDialog dialog = new AlertDialog.Builder(v.getContext(), R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
                .setView(layout)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        cancel.setOnClickListener(x -> dialog.dismiss());
        save.setOnClickListener(x -> {
            String newText = edit.getText().toString().trim();
            if (newText.isEmpty()) {
                Toast.makeText(v.getContext(), v.getContext().getString(R.string.empty_note), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                MuPDFLibrary.getAppInstance().updateNoteText(note.getId(), newText);
                note.setText(newText);
                notifyItemChanged(position);
                Toast.makeText(v.getContext(), "Not güncellendi", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Güncelleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        View rootOverlay = layout.findViewById(R.id.reader_note_popup);
        if (rootOverlay != null) {
            rootOverlay.setOnClickListener(xx -> dialog.dismiss());
        }

        dialog.show();
    }

    private void confirmDelete(View v, GPNote note, int position) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("Notu sil")
                .setMessage("Bu notu silmek istiyor musunuz?")
                .setNegativeButton("İptal", (d, i) -> d.dismiss())
                .setPositiveButton("Sil", (d, i) -> {
                    try {
                        MuPDFLibrary.getAppInstance().removeNote(note.getId());
                        notes.remove(position);
                        notifyItemRemoved(position);
                        // Not silindikten sonra buton görünürlüğünü güncelle
                        if (documentActivity != null) {
                            documentActivity.onNoteDeleted();
                        }
                    } catch (Exception ignored) {
                    }
                    d.dismiss();
                })
                .show();
    }
}