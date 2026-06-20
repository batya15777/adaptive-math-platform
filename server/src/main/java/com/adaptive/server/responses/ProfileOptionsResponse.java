package com.adaptive.server.responses;

import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * The selectable theme/language options with their display labels.
 * Built straight from the enums so the client never has to hardcode labels.
 */
public class ProfileOptionsResponse {

    public static class Option {
        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }

    private final List<Option> themes    = new ArrayList<>();
    private final List<Option> languages = new ArrayList<>();

    public ProfileOptionsResponse() {
        for (ProfileTheme theme : ProfileTheme.values()) {
            themes.add(new Option(theme.name(), theme.getLabel()));
        }
        for (Language language : Language.values()) {
            languages.add(new Option(language.name(), language.getLabel()));
        }
    }

    public List<Option> getThemes()    { return themes; }
    public List<Option> getLanguages() { return languages; }
}
