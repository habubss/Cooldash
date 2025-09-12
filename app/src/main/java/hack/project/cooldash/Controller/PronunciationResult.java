package hack.project.cooldash.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PronunciationResult {
    private String result;
    private String words;
    private List<Integer> accuracy;
    private String feedback_audio;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public List<Integer> getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(List<Integer> accuracy) {
        this.accuracy = accuracy;
    }

    public String getFeedback_audio() {
        return feedback_audio;
    }

    public void setFeedback_audio(String feedback_audio) {
        this.feedback_audio = feedback_audio;
    }

    public List<String> getWordsList() {
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(words.split(" "));
    }
}