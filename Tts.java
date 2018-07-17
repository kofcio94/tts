package *;

import android.speech.tts.TextToSpeech;
//fill empty imports
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

/**
 * Class prepared to bind everywhere with @Inject annotation and
 * generate speech from given (String) text and language (String eg. "bn_BD" or Locale)
 * <b>public</b> methods:
 * Tts(),
 * public void speak(String language, String text),
 * public void speak(Locale language, String text),
 * public String getAvailableLanguages(),
 * public void stopSpeak()
 * <p>
 * Author: Michał Jabłoński
 * Date Created: 14.06.2018
 */
public class Tts {

    private final static String TAG = Tts.class.getSimpleName();
    private final static String TTS_ENGINE = "com.google.android.tts";

    @Inject
    FragmentActivity context;

    private TextToSpeech textToSpeech;

    /**
     * Binds context from BaseActivity
     */
    public Tts() {
        Context.provideContext().into(this);
        initTts();
    }

    /**
     * Init tts engine and sets default language if possible
     */
    private void initTts() {
        textToSpeech = new TextToSpeech(context, null, TTS_ENGINE);

        Locale locale = Locale.getDefault();
        if (isLanguageAvailable(locale)) {
            textToSpeech.setLanguage(locale);
        }
    }

    /**
     * @param language - language from list received in method <b>getAvailableLanguages()</b>
     * @param text     - text to read
     */
    public void speak(String language, String text) {
        setSpeechLanguage(language);
        createAndStartSpeechQueue(text);
    }

    /**
     * @param language - language from list received in method <b>getAvailableLanguages()</b>
     * @param text     - text to read
     */
    public void speak(Locale language, String text) {
        setSpeechLanguage(language);
        createAndStartSpeechQueue(text);
    }

    /**
     * sets tts speech language
     */
    private void setSpeechLanguage(String language) {
        Locale locale = convertStringToLocale(language);

        if (locale != null && isLanguageAvailable(locale)) {
            Log.d(TAG, "language set: " + locale.toString());
            textToSpeech.setLanguage(locale);
        }
    }

    /**
     * sets tts speech language
     */
    private void setSpeechLanguage(Locale locale) {
        if (locale != null && isLanguageAvailable(locale)) {
            Log.d(TAG, "language set: " + locale.toString());
            textToSpeech.setLanguage(locale);
        }
    }

    /**
     * Converts String to locale
     *
     * @param language String eg."bn_BD"
     * @return Locale(String... langParams)
     */
    private Locale convertStringToLocale(String language) {
        String lang[] = language.split("_");

        Locale locale = null;
        switch (lang.length) {
            case 1:
                locale = new Locale(language);
                break;
            case 2:
                locale = new Locale(lang[0], lang[1]);
                break;
            case 3:
                locale = new Locale(lang[0], lang[1], lang[2]);
                break;
        }

        return locale;
    }

    /**
     * Modifies text to read: unescape characters
     * then splits String to its max length and starts reading
     *
     * @param text to read
     */
    private void createAndStartSpeechQueue(String text) {
        text = StringEscapeUtils.unescapeHtml4(text);
        int maxStringLength = TextToSpeech.getMaxSpeechInputLength();
        if (text.length() < maxStringLength) {
            startUtterance(text);
        } else {
            List<String> texts = splitTextAndCreateQueue(text, maxStringLength);
            startUtterance(texts);
        }
    }

    /**
     * Splits text longer than possible to read.
     *
     * @param text      to split
     * @param maxLength received from TextToSpeech.getMaxSpeechInputLength()
     * @return List<String> splitTexts;
     */
    private List<String> splitTextAndCreateQueue(String text, int maxLength) {
        final char splitChar = ' ';
        List<String> texts = new ArrayList<>();

        while (text.length() > maxLength) {

            int lastCharIndex = maxLength;
            char lastChar = text.charAt(lastCharIndex);

            while (lastChar != splitChar && maxLength > 0) {
                lastCharIndex--;
                lastChar = text.charAt(lastCharIndex);
            }

            if (lastCharIndex > 0) {
                texts.add(text.substring(0, lastCharIndex));
                text = text.substring(lastCharIndex + 1);
            } else {
                texts.add(text.substring(0, maxLength - 1));
                text = text.substring(maxLength);
            }
        }

        texts.add(text);

        return texts;
    }

    /**
     * Checks if language is available in text to speech
     *
     * @param locale
     * @return true if available of false if not
     */
    private boolean isLanguageAvailable(Locale locale) {
        try {
            int result = textToSpeech.isLanguageAvailable(locale);

            return result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts speaking for first element with queue flush and rest with queue add
     */
    private void startUtterance(@NotNull List<String> texts) {
        String initialText = texts.remove(0);
        startUtterance(initialText);
        for (String text : texts) {
            startUtterance(text, TextToSpeech.QUEUE_ADD);
        }
    }

    /**
     * Speaks with clearing actual startUtterance queue
     */
    private void startUtterance(String text) {
        startUtterance(text, TextToSpeech.QUEUE_FLUSH);
    }

    /**
     * Speaks in mode FLUSH or ADD
     */
    private void startUtterance(String text, int mode) {
        Log.d(TAG, "MODE " + "(" + mode + ") :" + text);
        textToSpeech.speak(text, mode, null, null);
    }

    /**
     * Interrupts the current utterance (whether played or rendered to file) and discards other
     * utterances in the queue.
     */
    public void stopSpeak() {
        Log.d(TAG, "Stop speech");
        textToSpeech.stop();
    }

    /**
     * @return language Json of available languages eg.
     * <pre>
     * {
     * "bn_BD" : "bengalski (Bangladesz)",
     * "de_DE" : "niemiecki (Niemcy)"
     * }
     * </pre>
     */
    public String getAvailableLanguages() {
        Map<String, String> languages = new HashMap<>();

        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            if (isLanguageAvailable(locale)) {
                String key = locale.toString();
                String value = locale.getDisplayName();

                languages.put(key, value);
            }
        }
        String languagesJson = languages.size() > 0 ? new JSONObject(languages).toString() : "{}";
        Logs.d(TAG, languagesJson);
        return languagesJson;
    }
}
