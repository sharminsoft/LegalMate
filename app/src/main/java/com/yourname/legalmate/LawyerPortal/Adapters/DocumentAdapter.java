package com.yourname.legalmate.LawyerPortal.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.legalmate.LawyerPortal.Models.AttachedDocument;
import com.yourname.legalmate.R;
import com.yourname.legalmate.utils.CloudinaryConfig;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<AttachedDocument> documentList;
    private OnDocumentActionListener listener;
    private Context context;

    public interface OnDocumentActionListener {
        void onDocumentRemove(AttachedDocument document);
        default void onDocumentView(AttachedDocument document) {}
        default void onDocumentDownload(AttachedDocument document) {}
    }

    public DocumentAdapter(List<AttachedDocument> documentList, OnDocumentActionListener listener) {
        this.documentList = documentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_attached_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        AttachedDocument document = documentList.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public class DocumentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivDocumentType;
        private TextView tvDocumentName;
        private TextView tvDocumentSize;
        private TextView tvDocumentStatus;
        private ImageView ivRemoveDocument;
        private ImageView ivViewDocument;
        private ProgressBar progressBar;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);

            ivDocumentType = itemView.findViewById(R.id.ivDocumentType);
            tvDocumentName = itemView.findViewById(R.id.tvDocumentName);
            tvDocumentSize = itemView.findViewById(R.id.tvDocumentSize);
            tvDocumentStatus = itemView.findViewById(R.id.tvDocumentStatus);
            ivRemoveDocument = itemView.findViewById(R.id.ivRemoveDocument);
            ivViewDocument = itemView.findViewById(R.id.ivViewDocument);

            progressBar = itemView.findViewById(R.id.progressBar);
        }

        public void bind(AttachedDocument document) {
            tvDocumentName.setText(document.getName());

            if (document.getFileSize() > 0) {
                tvDocumentSize.setText(document.getFormattedFileSize());
                tvDocumentSize.setVisibility(View.VISIBLE);
            } else {
                tvDocumentSize.setVisibility(View.GONE);
            }

            ivDocumentType.setImageResource(getDocumentTypeIcon(document));

            tvDocumentStatus.setText(getStatusText(document.getStatus()));
            tvDocumentStatus.setTextColor(ContextCompat.getColor(context, getStatusColor(document.getStatus())));

            if ("uploading".equals(document.getStatus())) {
                progressBar.setVisibility(View.VISIBLE);
                ivViewDocument.setVisibility(View.GONE);

            } else {
                progressBar.setVisibility(View.GONE);

                if ("uploaded".equals(document.getStatus()) && document.getCloudinaryUrl() != null) {
                    ivViewDocument.setVisibility(View.VISIBLE);

                } else {
                    ivViewDocument.setVisibility(View.GONE);

                }
            }

            ivViewDocument.setOnClickListener(v -> handleDocumentView(document));


            itemView.setOnClickListener(v -> {
                if ("uploaded".equals(document.getStatus())) {
                    handleDocumentView(document);
                }
            });
        }

        private void handleDocumentView(AttachedDocument document) {
            if (document.getCloudinaryUrl() == null || document.getCloudinaryUrl().isEmpty()) {
                Toast.makeText(context, "Document not available for viewing", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String viewingUrl;

                if (document.isPDF()) {
                    viewingUrl = CloudinaryConfig.generatePDFViewingURL(document.getCloudinaryUrl());
                } else {
                    viewingUrl = document.getCloudinaryUrl();
                }

                if (viewingUrl != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(viewingUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (document.isPDF()) {
                        intent.setType("application/pdf");
                    } else if (document.isImage()) {
                        intent.setType("image/*");
                    }

                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "No app found to view this document", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Unable to generate viewing URL", Toast.LENGTH_SHORT).show();
                }

                if (listener != null) {
                    listener.onDocumentView(document);
                }
            } catch (Exception e) {
                Toast.makeText(context, "Error opening document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void handleDocumentDownload(AttachedDocument document) {
            if (document.getCloudinaryUrl() == null || document.getCloudinaryUrl().isEmpty()) {
                Toast.makeText(context, "Document not available for download", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String downloadUrl;

                if (document.isPDF()) {
                    downloadUrl = CloudinaryConfig.generatePDFDownloadURL(document.getCloudinaryUrl(), document.getName());
                } else {
                    downloadUrl = document.getDownloadUrl();
                }

                if (downloadUrl != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(downloadUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                        Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "No app found to download this document", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Unable to generate download URL", Toast.LENGTH_SHORT).show();
                }

                if (listener != null) {
                    listener.onDocumentDownload(document);
                }
            } catch (Exception e) {
                Toast.makeText(context, "Error downloading document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private int getDocumentTypeIcon(AttachedDocument document) {
            if (document.isPDF()) {
                return R.drawable.ic_pdf;
            } else if (document.isImage()) {
                return R.drawable.ic_image;
            } else {
                return R.drawable.ic_document;
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "pending":
                    return "Pending";
                case "uploading":
                    return "Uploading...";
                case "uploaded":
                    return "Uploaded";
                case "failed":
                    return "Failed";
                default:
                    return "Unknown";
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "uploaded":
                    return R.color.success_color;
                case "uploading":
                    return R.color.warning_color;
                case "failed":
                    return R.color.error_color;
                default:
                    return R.color.text_secondary_color;
            }
        }
    }
}