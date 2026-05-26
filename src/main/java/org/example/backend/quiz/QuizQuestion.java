package org.example.backend.quiz;

import java.util.List;

/**
 * Interface untuk soal kuis — menerapkan ABSTRACTION.
 * Developer bisa membuat implementasi baru tanpa mengubah kode game.
 * Menerapkan POLYMORPHISM — berbagai tipe soal bisa diperlakukan sama.
 */
public interface QuizQuestion {

    /** Teks pertanyaan */
    String getQuestion();

    /** Daftar pilihan jawaban (A, B, C, D) */
    List<String> getChoices();

    /** Index jawaban benar (0-based) */
    int getCorrectIndex();

    /** Kategori soal (opsional, untuk filtering) */
    String getCategory();

    /** Cek apakah jawaban benar */
    default boolean isCorrect(int answerIndex) {
        return answerIndex == getCorrectIndex();
    }
}
