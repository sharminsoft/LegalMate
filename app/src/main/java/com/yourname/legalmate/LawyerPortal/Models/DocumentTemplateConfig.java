package com.yourname.legalmate.LawyerPortal.Models;

public class DocumentTemplateConfig {
    public static final String TYPE_PETITION = "petition";
    public static final String TYPE_AFFIDAVIT = "affidavit";
    public static final String TYPE_CONTRACT = "contract";

    public static DocumentField[] getPetitionFields() {
        return new DocumentField[]{
                new DocumentField("court_name", "Court Name", "আদালতের নাম", "text", true,
                        "Enter the name of the court", "আদালতের নাম লিখুন"),
                new DocumentField("case_number", "Case Number", "মামলা নম্বর", "text", false,
                        "Case number if available", "মামলা নম্বর (যদি থাকে)"),
                new DocumentField("petitioner_name", "Petitioner Name", "আবেদনকারীর নাম", "text", true,
                        "Full name of the petitioner", "আবেদনকারীর সম্পূর্ণ নাম"),
                new DocumentField("petitioner_father_name", "Father's Name", "পিতার নাম", "text", true,
                        "Father's name of petitioner", "আবেদনকারীর পিতার নাম"),
                new DocumentField("petitioner_address", "Petitioner Address", "আবেদনকারীর ঠিকানা", "textarea", true,
                        "Complete address of petitioner", "আবেদনকারীর সম্পূর্ণ ঠিকানা"),
                new DocumentField("respondent_name", "Respondent Name", "বিবাদীর নাম", "text", true,
                        "Full name of the respondent", "বিবাদীর সম্পূর্ণ নাম"),
                new DocumentField("respondent_address", "Respondent Address", "বিবাদীর ঠিকানা", "textarea", true,
                        "Complete address of respondent", "বিবাদীর সম্পূর্ণ ঠিকানা"),
                new DocumentField("petition_subject", "Subject of Petition", "আবেদনের বিষয়", "text", true,
                        "Brief subject of the petition", "আবেদনের সংক্ষিপ্ত বিষয়"),
                new DocumentField("facts", "Facts of the Case", "মামলার তথ্যাবলী", "textarea", true,
                        "Detailed facts and circumstances", "বিস্তারিত তথ্য ও পরিস্থিতি"),
                new DocumentField("grounds", "Grounds", "ভিত্তি", "textarea", true,
                        "Legal grounds for the petition", "আবেদনের আইনগত ভিত্তি"),
                new DocumentField("prayer", "Prayer", "প্রার্থনা", "textarea", true,
                        "What relief you are seeking", "আপনি কী প্রতিকার চাচ্ছেন"),
                new DocumentField("date", "Date", "তারিখ", "date", true,
                        "Date of filing", "দাখিলের তারিখ")
        };
    }

    public static DocumentField[] getAffidavitFields() {
        return new DocumentField[]{
                new DocumentField("deponent_name", "Deponent Name", "শপথকারীর নাম", "text", true,
                        "Full name of the person making affidavit", "হলফনামাকারীর সম্পূর্ণ নাম"),
                new DocumentField("deponent_father_name", "Father's Name", "পিতার নাম", "text", true,
                        "Father's full name", "পিতার সম্পূর্ণ নাম"),
                new DocumentField("deponent_age", "Age", "বয়স", "number", true,
                        "Age in years", "বছরে বয়স"),
                new DocumentField("deponent_occupation", "Occupation", "পেশা", "text", true,
                        "Current occupation or profession", "বর্তমান পেশা বা জীবিকা"),
                new DocumentField("deponent_address", "Address", "ঠিকানা", "textarea", true,
                        "Complete residential address", "সম্পূর্ণ আবাসিক ঠিকানা"),
                new DocumentField("affidavit_subject", "Subject", "বিষয়", "text", true,
                        "Subject matter of the affidavit", "হলফনামার বিষয়বস্তু"),
                new DocumentField("statement_facts", "Statement of Facts", "তথ্যের বিবৃতি", "textarea", true,
                        "Detailed factual statement", "বিস্তারিত তথ্যভিত্তিক বিবৃতি"),
                new DocumentField("verification", "Verification Statement", "সত্যায়ন বিবৃতি", "textarea", false,
                        "Verification of the facts stated", "উল্লিখিত তথ্যের সত্যায়ন"),
                new DocumentField("place", "Place", "স্থান", "text", true,
                        "Place where affidavit is made", "হলফনামা সম্পাদনের স্থান"),
                new DocumentField("date", "Date", "তারিখ", "date", true,
                        "Date of making affidavit", "হলফনামা সম্পাদনের তারিখ")
        };
    }

    public static DocumentField[] getContractFields() {
        return new DocumentField[]{
                new DocumentField("contract_title", "Contract Title", "চুক্তির শিরোনাম", "text", true,
                        "Title or nature of the contract", "চুক্তির শিরোনাম বা প্রকৃতি"),
                new DocumentField("party1_name", "First Party Name", "প্রথম পক্ষের নাম", "text", true,
                        "Full name of the first party", "প্রথম পক্ষের সম্পূর্ণ নাম"),
                new DocumentField("party1_father_name", "First Party Father's Name", "প্রথম পক্ষের পিতার নাম", "text", true,
                        "Father's name of first party", "প্রথম পক্ষের পিতার নাম"),
                new DocumentField("party1_address", "First Party Address", "প্রথম পক্ষের ঠিকানা", "textarea", true,
                        "Complete address of first party", "প্রথম পক্ষের সম্পূর্ণ ঠিকানা"),
                new DocumentField("party2_name", "Second Party Name", "দ্বিতীয় পক্ষের নাম", "text", true,
                        "Full name of the second party", "দ্বিতীয় পক্ষের সম্পূর্ণ নাম"),
                new DocumentField("party2_father_name", "Second Party Father's Name", "দ্বিতীয় পক্ষের পিতার নাম", "text", true,
                        "Father's name of second party", "দ্বিতীয় পক্ষের পিতার নাম"),
                new DocumentField("party2_address", "Second Party Address", "দ্বিতীয় পক্ষের ঠিকানা", "textarea", true,
                        "Complete address of second party", "দ্বিতীয় পক্ষের সম্পূর্ণ ঠিকানা"),
                new DocumentField("consideration", "Consideration", "বিবেচনা", "text", false,
                        "Consideration for the contract", "চুক্তির বিবেচনা"),
                new DocumentField("contract_amount", "Contract Amount", "চুক্তির পরিমাণ", "text", false,
                        "Amount in numbers and words", "সংখ্যায় ও কথায় পরিমাণ"),
                new DocumentField("contract_duration", "Duration", "মেয়াদ", "text", false,
                        "Duration or validity period", "চুক্তির মেয়াদ বা বৈধতার সময়কাল"),
                new DocumentField("terms_conditions", "Terms & Conditions", "শর্তাবলী", "textarea", true,
                        "Detailed terms and conditions", "বিস্তারিত শর্তাবলী"),
                new DocumentField("obligations_party1", "Obligations of First Party", "প্রথম পক্ষের দায়িত্ব", "textarea", false,
                        "Specific obligations of first party", "প্রথম পক্ষের নির্দিষ্ট দায়িত্বসমূহ"),
                new DocumentField("obligations_party2", "Obligations of Second Party", "দ্বিতীয় পক্ষের দায়িত্ব", "textarea", false,
                        "Specific obligations of second party", "দ্বিতীয় পক্ষের নির্দিষ্ট দায়িত্বসমূহ"),
                new DocumentField("breach_clause", "Breach Clause", "লঙ্ঘনের ধারা", "textarea", false,
                        "What happens in case of breach", "লঙ্ঘনের ক্ষেত্রে কী হবে"),
                new DocumentField("dispute_resolution", "Dispute Resolution", "বিরোধ নিষ্পত্তি", "textarea", false,
                        "How disputes will be resolved", "কীভাবে বিরোধ নিষ্পত্তি হবে"),
                new DocumentField("governing_law", "Governing Law", "প্রযোজ্য আইন", "text", false,
                        "Which law will govern this contract", "কোন আইন এই চুক্তি নিয়ন্ত্রণ করবে"),
                new DocumentField("special_clauses", "Special Clauses", "বিশেষ ধারাসমূহ", "textarea", false,
                        "Any special clauses or conditions", "কোন বিশেষ ধারা বা শর্ত"),
                new DocumentField("witness1_name", "First Witness Name", "প্রথম সাক্ষীর নাম", "text", false,
                        "Name of first witness", "প্রথম সাক্ষীর নাম"),
                new DocumentField("witness2_name", "Second Witness Name", "দ্বিতীয় সাক্ষীর নাম", "text", false,
                        "Name of second witness", "দ্বিতীয় সাক্ষীর নাম"),
                new DocumentField("place", "Place", "স্থান", "text", true,
                        "Place of contract execution", "চুক্তি সম্পাদনের স্থান"),
                new DocumentField("date", "Date", "তারিখ", "date", true,
                        "Date of contract execution", "চুক্তি সম্পাদনের তারিখ")
        };
    }

    public static DocumentField[] getFieldsByType(String type) {
        switch (type) {
            case TYPE_PETITION:
                return getPetitionFields();
            case TYPE_AFFIDAVIT:
                return getAffidavitFields();
            case TYPE_CONTRACT:
                return getContractFields();
            default:
                return new DocumentField[0];
        }
    }

    public static String getTypeName(String type, boolean bangla) {
        switch (type) {
            case TYPE_PETITION:
                return bangla ? "আবেদন" : "Petition";
            case TYPE_AFFIDAVIT:
                return bangla ? "হলফনামা" : "Affidavit";
            case TYPE_CONTRACT:
                return bangla ? "চুক্তি" : "Contract";
            default:
                return bangla ? "নথি" : "Document";
        }
    }
}