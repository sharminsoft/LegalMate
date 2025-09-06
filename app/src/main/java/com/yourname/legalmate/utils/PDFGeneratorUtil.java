package com.yourname.legalmate.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import com.yourname.legalmate.LawyerPortal.Models.DocumentField;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplate;
import com.yourname.legalmate.LawyerPortal.Models.DocumentTemplateConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PDFGeneratorUtil {

    private static final String TAG = "PDFGeneratorUtil";

    // A4 page dimensions in points (72 DPI)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 50;
    private static final int LINE_HEIGHT = 20;
    private static final int SECTION_SPACING = 25;
    private static final int FIELD_SPACING = 15;

    // Text sizes
    private static final int TITLE_TEXT_SIZE = 20;
    private static final int HEADER_TEXT_SIZE = 16;
    private static final int SECTION_TEXT_SIZE = 14;
    private static final int BODY_TEXT_SIZE = 12;
    private static final int LABEL_TEXT_SIZE = 11;

    public static class PDFResult {
        public boolean success;
        public String filePath;
        public String errorMessage;

        public PDFResult(boolean success, String filePath, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }
    }

    public static PDFResult generateDocumentPDF(Context context, DocumentTemplate document, String language) {
        PdfDocument pdfDocument = null;
        FileOutputStream fos = null;

        try {
            // Create PDF document
            pdfDocument = new PdfDocument();

            // Create initial page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page currentPage = pdfDocument.startPage(pageInfo);
            Canvas canvas = currentPage.getCanvas();

            // Setup paint objects with better styling
            Paint titlePaint = createPaint(TITLE_TEXT_SIZE, true, false);
            Paint headerPaint = createPaint(HEADER_TEXT_SIZE, true, false);
            Paint sectionPaint = createPaint(SECTION_TEXT_SIZE, true, false);
            Paint labelPaint = createPaint(LABEL_TEXT_SIZE, true, false);
            Paint bodyPaint = createPaint(BODY_TEXT_SIZE, false, false);
            Paint separatorPaint = createPaint(1, false, true);

            int currentY = MARGIN + 40;
            int pageNumber = 1;

            // Document header
            currentY = drawDocumentHeader(canvas, document, language, currentY, titlePaint, headerPaint, bodyPaint, separatorPaint);

            // Check if we need a new page after header
            if (currentY > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(currentPage);
                pageNumber++;
                currentPage = createNewPage(pdfDocument, pageNumber);
                canvas = currentPage.getCanvas();
                currentY = MARGIN + 40;
            }

            // Document content
            PDFPageResult pageResult = drawDocumentContent(canvas, pdfDocument, document, language,
                    currentY, pageNumber, sectionPaint, labelPaint, bodyPaint, currentPage);

            currentPage = pageResult.currentPage;
            canvas = pageResult.canvas;
            currentY = pageResult.currentY;
            pageNumber = pageResult.pageNumber;

            // Document footer
            drawDocumentFooter(canvas, document, language, bodyPaint);

            // Finish current page
            pdfDocument.finishPage(currentPage);

            // Save PDF with better file naming
            String fileName = generateFileName(document, language);
            File documentsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "LegalMate_PDFs");

            // Create directory if it doesn't exist
            if (!documentsDir.exists()) {
                documentsDir.mkdirs();
            }

            File file = new File(documentsDir, fileName);
            fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);

            return new PDFResult(true, file.getAbsolutePath(), null);

        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF", e);
            return new PDFResult(false, null, "PDF generation failed: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error generating PDF", e);
            return new PDFResult(false, null, "Unexpected error: " + e.getMessage());
        } finally {
            // Clean up resources
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file output stream", e);
                }
            }
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private static class PDFPageResult {
        PdfDocument.Page currentPage;
        Canvas canvas;
        int currentY;
        int pageNumber;

        PDFPageResult(PdfDocument.Page page, Canvas canvas, int y, int pageNum) {
            this.currentPage = page;
            this.canvas = canvas;
            this.currentY = y;
            this.pageNumber = pageNum;
        }
    }

    private static Paint createPaint(int textSize, boolean bold, boolean isLine) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);

        if (bold) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        if (isLine) {
            paint.setStrokeWidth(1.0f);
        }

        return paint;
    }

    private static PdfDocument.Page createNewPage(PdfDocument pdfDocument, int pageNumber) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        return pdfDocument.startPage(pageInfo);
    }

    private static int drawDocumentHeader(Canvas canvas, DocumentTemplate document, String language,
                                          int startY, Paint titlePaint, Paint headerPaint, Paint bodyPaint, Paint separatorPaint) {

        int currentY = startY;

        // Main title
        String title = language.equals("bn") ? document.getTitleBangla() : document.getTitle();
        canvas.drawText(title, MARGIN, currentY, titlePaint);
        currentY += 50;

        // Document type and date in a row
        String typeLabel = getDocumentTypeDisplay(document.getType(), language);
        canvas.drawText(typeLabel, MARGIN, currentY, headerPaint);

        // Date on the right side
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy",
                language.equals("bn") ? new Locale("bn", "BD") : Locale.getDefault());
        String dateText = language.equals("bn") ? "তারিখ: " + sdf.format(new Date()) : "Date: " + sdf.format(new Date());
        float dateWidth = bodyPaint.measureText(dateText);
        canvas.drawText(dateText, PAGE_WIDTH - MARGIN - dateWidth, currentY, bodyPaint);
        currentY += 35;

        // Separator line
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, separatorPaint);
        currentY += SECTION_SPACING;

        return currentY;
    }

    private static void drawDocumentFooter(Canvas canvas, DocumentTemplate document, String language, Paint bodyPaint) {
        int footerY = PAGE_HEIGHT - MARGIN;

        // Generated timestamp
        SimpleDateFormat timestampFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String footerText = language.equals("bn") ?
                "তৈরি: " + timestampFormat.format(new Date()) + " | LegalMate" :
                "Generated: " + timestampFormat.format(new Date()) + " | LegalMate";

        float footerWidth = bodyPaint.measureText(footerText);
        canvas.drawText(footerText, (PAGE_WIDTH - footerWidth) / 2, footerY, bodyPaint);
    }

    private static PDFPageResult drawDocumentContent(Canvas canvas, PdfDocument pdfDocument,
                                                     DocumentTemplate document, String language, int startY, int pageNumber,
                                                     Paint sectionPaint, Paint labelPaint, Paint bodyPaint, PdfDocument.Page currentPage) {

        int currentY = startY;

        DocumentField[] fields = DocumentTemplateConfig.getFieldsByType(document.getType());
        Map<String, Object> formData = document.getFormData();

        // Create the initial page result with the passed current page
        PDFPageResult result = new PDFPageResult(currentPage, canvas, currentY, pageNumber);

        switch (document.getType()) {
            case DocumentTemplateConfig.TYPE_PETITION:
                result = drawPetitionContent(canvas, pdfDocument, fields, formData, language,
                        result, sectionPaint, labelPaint, bodyPaint);
                break;
            case DocumentTemplateConfig.TYPE_AFFIDAVIT:
                result = drawAffidavitContent(canvas, pdfDocument, fields, formData, language,
                        result, sectionPaint, labelPaint, bodyPaint);
                break;
            case DocumentTemplateConfig.TYPE_CONTRACT:
                result = drawContractContent(canvas, pdfDocument, fields, formData, language,
                        result, sectionPaint, labelPaint, bodyPaint);
                break;
        }

        return result;
    }

    private static PDFPageResult drawPetitionContent(Canvas canvas, PdfDocument pdfDocument,
                                                     DocumentField[] fields, Map<String, Object> formData, String language,
                                                     PDFPageResult currentResult, Paint sectionPaint, Paint labelPaint, Paint bodyPaint) {

        // Court Information Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "আদালতের তথ্য" : "COURT INFORMATION", sectionPaint);

        String[] courtFields = {"court_name", "case_number"};
        for (String fieldKey : courtFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        currentResult = addSpacing(currentResult, SECTION_SPACING);

        // Parties Information Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "পক্ষগণের তথ্য" : "PARTIES INFORMATION", sectionPaint);

        String[] partyFields = {"petitioner_name", "petitioner_address", "respondent_name", "respondent_address"};
        for (String fieldKey : partyFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        currentResult = addSpacing(currentResult, SECTION_SPACING);

        // Petition Details Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "আবেদনের বিস্তারিত" : "PETITION DETAILS", sectionPaint);

        String[] detailFields = {"petition_subject", "facts", "prayer"};
        for (String fieldKey : detailFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        return currentResult;
    }

    private static PDFPageResult drawAffidavitContent(Canvas canvas, PdfDocument pdfDocument,
                                                      DocumentField[] fields, Map<String, Object> formData, String language,
                                                      PDFPageResult currentResult, Paint sectionPaint, Paint labelPaint, Paint bodyPaint) {

        // Deponent Information Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "শপথকারীর তথ্য" : "DEPONENT INFORMATION", sectionPaint);

        String[] deponentFields = {"deponent_name", "deponent_father_name", "deponent_age",
                "deponent_occupation", "deponent_address"};
        for (String fieldKey : deponentFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        currentResult = addSpacing(currentResult, SECTION_SPACING);

        // Affidavit Content Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "হলফনামার বিষয়বস্তু" : "AFFIDAVIT CONTENT", sectionPaint);

        String[] contentFields = {"affidavit_subject", "statement"};
        for (String fieldKey : contentFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        return currentResult;
    }

    private static PDFPageResult drawContractContent(Canvas canvas, PdfDocument pdfDocument,
                                                     DocumentField[] fields, Map<String, Object> formData, String language,
                                                     PDFPageResult currentResult, Paint sectionPaint, Paint labelPaint, Paint bodyPaint) {

        // Contract Information Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "চুক্তির তথ্য" : "CONTRACT INFORMATION", sectionPaint);

        currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, "contract_title", fields, formData,
                language, labelPaint, bodyPaint);

        currentResult = addSpacing(currentResult, SECTION_SPACING);

        // Contracting Parties Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "চুক্তিকারী পক্ষসমূহ" : "CONTRACTING PARTIES", sectionPaint);

        String[] partyFields = {"party1_name", "party1_address", "party2_name", "party2_address"};
        for (String fieldKey : partyFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        currentResult = addSpacing(currentResult, SECTION_SPACING);

        // Contract Terms Section
        currentResult = drawSectionWithPageCheck(pdfDocument, currentResult,
                language.equals("bn") ? "চুক্তির শর্তাবলী" : "CONTRACT TERMS", sectionPaint);

        String[] termFields = {"contract_amount", "contract_duration", "terms_conditions", "special_clauses"};
        for (String fieldKey : termFields) {
            currentResult = drawFieldWithPageCheck(pdfDocument, currentResult, fieldKey, fields, formData,
                    language, labelPaint, bodyPaint);
        }

        return currentResult;
    }

    private static PDFPageResult drawSectionWithPageCheck(PdfDocument pdfDocument, PDFPageResult currentResult,
                                                          String title, Paint sectionPaint) {

        // Check if we need a new page (section title + some content space)
        if (currentResult.currentY > PAGE_HEIGHT - 150) {
            pdfDocument.finishPage(currentResult.currentPage);
            currentResult.pageNumber++;
            currentResult.currentPage = createNewPage(pdfDocument, currentResult.pageNumber);
            currentResult.canvas = currentResult.currentPage.getCanvas();
            currentResult.currentY = MARGIN + 40;
        }

        currentResult.canvas.drawText(title, MARGIN, currentResult.currentY, sectionPaint);
        currentResult.currentY += SECTION_SPACING;

        return currentResult;
    }

    private static PDFPageResult drawFieldWithPageCheck(PdfDocument pdfDocument, PDFPageResult currentResult,
                                                        String fieldKey, DocumentField[] fields, Map<String, Object> formData, String language,
                                                        Paint labelPaint, Paint bodyPaint) {

        DocumentField field = findField(fields, fieldKey);
        if (field == null || !formData.containsKey(fieldKey)) {
            return currentResult;
        }

        Object valueObj = formData.get(fieldKey);
        String value = valueObj != null ? valueObj.toString().trim() : "";

        if (value.isEmpty()) {
            return currentResult;
        }

        String label = language.equals("bn") ? field.getLabelBangla() : field.getLabel();

        // Calculate required space for this field
        int requiredSpace = LINE_HEIGHT + FIELD_SPACING; // Label space
        if (field.getType().equals("textarea")) {
            String[] words = value.split(" ");
            int estimatedLines = Math.max(1, (int) Math.ceil(bodyPaint.measureText(value) / (PAGE_WIDTH - MARGIN - 20)));
            requiredSpace += estimatedLines * LINE_HEIGHT + 10;
        } else {
            requiredSpace += LINE_HEIGHT;
        }

        // Check if we need a new page
        if (currentResult.currentY + requiredSpace > PAGE_HEIGHT - 100) {
            pdfDocument.finishPage(currentResult.currentPage);
            currentResult.pageNumber++;
            currentResult.currentPage = createNewPage(pdfDocument, currentResult.pageNumber);
            currentResult.canvas = currentResult.currentPage.getCanvas();
            currentResult.currentY = MARGIN + 40;
        }

        // Draw label
        currentResult.canvas.drawText(label + ":", MARGIN, currentResult.currentY, labelPaint);
        currentResult.currentY += FIELD_SPACING;

        // Draw value
        if (field.getType().equals("textarea")) {
            currentResult.currentY = drawMultilineText(currentResult.canvas, value, MARGIN + 10,
                    currentResult.currentY, bodyPaint, PAGE_WIDTH - MARGIN - 20);
        } else {
            currentResult.canvas.drawText(value, MARGIN + 10, currentResult.currentY, bodyPaint);
            currentResult.currentY += LINE_HEIGHT;
        }

        currentResult.currentY += 10; // Field spacing

        return currentResult;
    }

    private static PDFPageResult addSpacing(PDFPageResult currentResult, int spacing) {
        currentResult.currentY += spacing;
        return currentResult;
    }

    private static int drawMultilineText(Canvas canvas, String text, int x, int startY, Paint paint, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return startY;
        }

        String[] paragraphs = text.split("\n");
        int currentY = startY;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                currentY += LINE_HEIGHT / 2; // Half line for empty paragraphs
                continue;
            }

            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
                float testWidth = paint.measureText(testLine);

                if (testWidth > maxWidth && currentLine.length() > 0) {
                    // Draw current line and start new line
                    canvas.drawText(currentLine.toString(), x, currentY, paint);
                    currentY += LINE_HEIGHT;
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            // Draw remaining text in current line
            if (currentLine.length() > 0) {
                canvas.drawText(currentLine.toString(), x, currentY, paint);
                currentY += LINE_HEIGHT;
            }

            currentY += 5; // Small spacing between paragraphs
        }

        return currentY;
    }

    private static DocumentField findField(DocumentField[] fields, String key) {
        if (fields == null || key == null) {
            return null;
        }

        for (DocumentField field : fields) {
            if (field != null && key.equals(field.getKey())) {
                return field;
            }
        }
        return null;
    }

    private static String getDocumentTypeDisplay(String type, String language) {
        if (type == null) return language.equals("bn") ? "দলিল" : "DOCUMENT";

        switch (type) {
            case DocumentTemplateConfig.TYPE_PETITION:
                return language.equals("bn") ? "আবেদনপত্র" : "PETITION";
            case DocumentTemplateConfig.TYPE_AFFIDAVIT:
                return language.equals("bn") ? "হলফনামা" : "AFFIDAVIT";
            case DocumentTemplateConfig.TYPE_CONTRACT:
                return language.equals("bn") ? "চুক্তিপত্র" : "CONTRACT";
            default:
                return language.equals("bn") ? "দলিল" : "DOCUMENT";
        }
    }

    private static String generateFileName(DocumentTemplate document, String language) {
        if (document == null) {
            return "document_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        }

        String type = document.getType() != null ? document.getType() : "document";
        String title = "";

        // Try to get a meaningful title from the document
        if (document.getFormData() != null) {
            Map<String, Object> formData = document.getFormData();

            // Look for common title fields based on document type
            switch (type) {
                case DocumentTemplateConfig.TYPE_PETITION:
                    if (formData.containsKey("petition_subject")) {
                        Object subjectObj = formData.get("petition_subject");
                        title = subjectObj != null ? "_" + sanitizeFileName(subjectObj.toString()) : "";
                    }
                    break;
                case DocumentTemplateConfig.TYPE_AFFIDAVIT:
                    if (formData.containsKey("deponent_name")) {
                        Object nameObj = formData.get("deponent_name");
                        title = nameObj != null ? "_" + sanitizeFileName(nameObj.toString()) : "";
                    }
                    break;
                case DocumentTemplateConfig.TYPE_CONTRACT:
                    if (formData.containsKey("contract_title")) {
                        Object titleObj = formData.get("contract_title");
                        title = titleObj != null ? "_" + sanitizeFileName(titleObj.toString()) : "";
                    }
                    break;
            }
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return type + title + "_" + timestamp + ".pdf";
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "";

        // Remove invalid characters and limit length
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9\\s\\-_]", "")
                .replaceAll("\\s+", "_")
                .trim();

        // Limit length to avoid too long file names
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }

        return sanitized;
    }
}