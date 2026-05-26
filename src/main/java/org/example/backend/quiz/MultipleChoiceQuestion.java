package org.example.backend.quiz;

import java.util.List;

/**
 * Implementasi konkret QuizQuestion — pilihan ganda.
 * Menerapkan ENCAPSULATION (field private, akses via getter).
 * Menerapkan INHERITANCE dari interface QuizQuestion.
 */
public class MultipleChoiceQuestion implements QuizQuestion {

    private final String question;
    private final List<String> choices;
    private final int correctIndex;
    private final String category;

    public MultipleChoiceQuestion(String question, List<String> choices,
                                   int correctIndex, String category) {
        if (choices == null || choices.size() < 2) {
            throw new IllegalArgumentException("Minimal 2 pilihan jawaban");
        }
        if (correctIndex < 0 || correctIndex >= choices.size()) {
            throw new IllegalArgumentException("Index jawaban tidak valid");
        }
        this.question = question;
        this.choices = List.copyOf(choices); // immutable
        this.correctIndex = correctIndex;
        this.category = category;
    }

    @Override
    public String getQuestion() { return question; }

    @Override
    public List<String> getChoices() { return choices; }

    @Override
    public int getCorrectIndex() { return correctIndex; }

    @Override
    public String getCategory() { return category; }
}
