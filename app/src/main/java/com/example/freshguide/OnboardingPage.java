package com.example.freshguide;

public class OnboardingPage {
    private final int accentColorRes;
    private final int heroIconRes;
    private final int primaryChipIconRes;
    private final int secondaryChipIconRes;
    private final int tertiaryChipIconRes;
    private final String eyebrow;
    private final String title;
    private final String description;
    private final String primaryChipLabel;
    private final String secondaryChipLabel;
    private final String tertiaryChipLabel;

    public OnboardingPage(
            int accentColorRes,
            int heroIconRes,
            String eyebrow,
            String title,
            String description,
            int primaryChipIconRes,
            String primaryChipLabel,
            int secondaryChipIconRes,
            String secondaryChipLabel,
            int tertiaryChipIconRes,
            String tertiaryChipLabel) {
        this.accentColorRes = accentColorRes;
        this.heroIconRes = heroIconRes;
        this.eyebrow = eyebrow;
        this.title = title;
        this.description = description;
        this.primaryChipIconRes = primaryChipIconRes;
        this.primaryChipLabel = primaryChipLabel;
        this.secondaryChipIconRes = secondaryChipIconRes;
        this.secondaryChipLabel = secondaryChipLabel;
        this.tertiaryChipIconRes = tertiaryChipIconRes;
        this.tertiaryChipLabel = tertiaryChipLabel;
    }

    public int getAccentColorRes() {
        return accentColorRes;
    }

    public int getHeroIconRes() {
        return heroIconRes;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrimaryChipIconRes() {
        return primaryChipIconRes;
    }

    public String getPrimaryChipLabel() {
        return primaryChipLabel;
    }

    public int getSecondaryChipIconRes() {
        return secondaryChipIconRes;
    }

    public String getSecondaryChipLabel() {
        return secondaryChipLabel;
    }

    public int getTertiaryChipIconRes() {
        return tertiaryChipIconRes;
    }

    public String getTertiaryChipLabel() {
        return tertiaryChipLabel;
    }
}
