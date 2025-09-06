package com.yourname.legalmate.LawyerPortal.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplate;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplateConfig;
import com.yourname.legalmate.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DocumentTemplateAdapter extends RecyclerView.Adapter<DocumentTemplateAdapter.DocumentViewHolder> {

    private List<DocumentTemplate> documentList;
    private String currentLanguage;
    private OnDocumentClickListener onDocumentClickListener;

    public interface OnDocumentClickListener {
        void onDocumentClick(DocumentTemplate document);
    }

    public DocumentTemplateAdapter(List<DocumentTemplate> documentList, String currentLanguage,
                                   OnDocumentClickListener onDocumentClickListener) {
        this.documentList = documentList;
        this.currentLanguage = currentLanguage;
        this.onDocumentClickListener = onDocumentClickListener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document_template, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        DocumentTemplate document = documentList.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public void updateLanguage(String language) {
        this.currentLanguage = language;
        notifyDataSetChanged();
    }

    public class DocumentViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardDocument;
        private ImageView imgDocumentIcon;
        private TextView txtDocumentTitle;
        private TextView txtDocumentType;
        private TextView txtCreatedDate;
        private TextView txtStatus;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDocument = itemView.findViewById(R.id.cardDocument);
            imgDocumentIcon = itemView.findViewById(R.id.imgDocumentIcon);
            txtDocumentTitle = itemView.findViewById(R.id.txtDocumentTitle);
            txtDocumentType = itemView.findViewById(R.id.txtDocumentType);
            txtCreatedDate = itemView.findViewById(R.id.txtCreatedDate);
            txtStatus = itemView.findViewById(R.id.txtStatus);

            cardDocument.setOnClickListener(v -> {
                if (onDocumentClickListener != null) {
                    onDocumentClickListener.onDocumentClick(documentList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(DocumentTemplate document) {
            // Set title based on language
            if (currentLanguage.equals("bn")) {
                txtDocumentTitle.setText(document.getTitleBangla());
            } else {
                txtDocumentTitle.setText(document.getTitle());
            }

            // Set document type with icon
            setDocumentTypeAndIcon(document.getType());

            // Format and set creation date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy",
                    currentLanguage.equals("bn") ? new Locale("bn", "BD") : Locale.getDefault());
            String dateText = sdf.format(document.getCreatedAt());
            txtCreatedDate.setText(currentLanguage.equals("bn") ?
                    "তৈরি: " + dateText : "Created: " + dateText);

            // Set status
            setStatus(document.getStatus());
        }

        private void setDocumentTypeAndIcon(String type) {
            switch (type) {
                case DocumentTemplateConfig.TYPE_PETITION:
                    imgDocumentIcon.setImageResource(R.drawable.ic_petition);
                    txtDocumentType.setText(currentLanguage.equals("bn") ? "আবেদন" : "Petition");
                    break;
                case DocumentTemplateConfig.TYPE_AFFIDAVIT:
                    imgDocumentIcon.setImageResource(R.drawable.ic_affidavit);
                    txtDocumentType.setText(currentLanguage.equals("bn") ? "হলফনামা" : "Affidavit");
                    break;
                case DocumentTemplateConfig.TYPE_CONTRACT:
                    imgDocumentIcon.setImageResource(R.drawable.ic_contract);
                    txtDocumentType.setText(currentLanguage.equals("bn") ? "চুক্তি" : "Contract");
                    break;
                default:
                    imgDocumentIcon.setImageResource(R.drawable.ic_document_default);
                    txtDocumentType.setText(currentLanguage.equals("bn") ? "নথি" : "Document");
                    break;
            }
        }

        private void setStatus(String status) {
            switch (status.toLowerCase()) {
                case "draft":
                    txtStatus.setText(currentLanguage.equals("bn") ? "খসড়া" : "Draft");
                    txtStatus.setBackgroundResource(R.drawable.bg_status_draft);
                    break;
                case "completed":
                    txtStatus.setText(currentLanguage.equals("bn") ? "সম্পূর্ণ" : "Completed");
                    txtStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case "pending":
                    txtStatus.setText(currentLanguage.equals("bn") ? "অপেক্ষমান" : "Pending");
                    txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
                default:
                    txtStatus.setText(currentLanguage.equals("bn") ? "খসড়া" : "Draft");
                    txtStatus.setBackgroundResource(R.drawable.bg_status_draft);
                    break;
            }
        }
    }
}