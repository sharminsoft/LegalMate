package com.yourname.legalmate.GeneralPersonPortal.Models;

public class SliderItem {
    private String SliderImageUrl;
    private String SliderTitle;
    private String SliderSubtitle;

    public SliderItem() {
    }

    public SliderItem(String sliderImageUrl, String sliderTitle, String sliderSubtitle) {
        SliderImageUrl = sliderImageUrl;
        SliderTitle = sliderTitle;
        SliderSubtitle = sliderSubtitle;
    }

    public String getSliderImageUrl() {
        return SliderImageUrl;
    }

    public void setSliderImageUrl(String sliderImageUrl) {
        SliderImageUrl = sliderImageUrl;
    }

    public String getSliderTitle() {
        return SliderTitle;
    }

    public void setSliderTitle(String sliderTitle) {
        SliderTitle = sliderTitle;
    }

    public String getSliderSubtitle() {
        return SliderSubtitle;
    }

    public void setSliderSubtitle(String sliderSubtitle) {
        SliderSubtitle = sliderSubtitle;
    }
}