package org.example.backendkelaspbo.quiz;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bank soal kuis — modular, developer bisa tambah/ubah soal di sini
 * tanpa menyentuh kode game sama sekali.
 *
 * Menerapkan ABSTRACTION — game hanya tahu QuizQuestion interface,
 * tidak peduli implementasi konkretnya.
 */
@Component
public class QuizBank {

    private final List<QuizQuestion> questions = new ArrayList<>();
    private final Random random = new Random();

    public QuizBank() {
        loadQuestions();
    }

    /**
     * =====================================================
     * TAMBAH / UBAH SOAL DI SINI — tidak perlu ubah kode lain
     * =====================================================
     */
    private void loadQuestions() {
        // --- Matematika ---
        add("Berapa hasil dari 7 x 8?",
                List.of("54", "56", "48", "64"), 1, "Matematika");

        add("Berapa akar kuadrat dari 144?",
                List.of("10", "11", "12", "13"), 2, "Matematika");

        add("Berapa hasil dari 15 + 27?",
                List.of("40", "41", "42", "43"), 2, "Matematika");

        add("Berapa hasil dari 100 ÷ 4?",
                List.of("20", "25", "30", "40"), 1, "Matematika");

        add("Berapa hasil dari 3³ (3 pangkat 3)?",
                List.of("9", "18", "27", "36"), 2, "Matematika");

        // --- Sains ---
        add("Planet apa yang paling dekat dengan Matahari?",
                List.of("Venus", "Bumi", "Mars", "Merkurius"), 3, "Sains");

        add("Apa rumus kimia air?",
                List.of("CO2", "H2O", "O2", "NaCl"), 1, "Sains");

        add("Berapa kecepatan cahaya (km/s)?",
                List.of("100.000", "200.000", "300.000", "400.000"), 2, "Sains");

        add("Hewan apa yang memiliki leher terpanjang?",
                List.of("Unta", "Jerapah", "Kuda", "Gajah"), 1, "Sains");

        add("Gas apa yang paling banyak di atmosfer Bumi?",
                List.of("Oksigen", "Karbon Dioksida", "Nitrogen", "Hidrogen"), 2, "Sains");

        // --- Geografi ---
        add("Ibu kota Indonesia adalah?",
                List.of("Surabaya", "Bandung", "Jakarta", "Medan"), 2, "Geografi");

        add("Gunung tertinggi di dunia adalah?",
                List.of("K2", "Everest", "Kilimanjaro", "Elbrus"), 1, "Geografi");

        add("Benua terluas di dunia adalah?",
                List.of("Afrika", "Amerika", "Asia", "Eropa"), 2, "Geografi");

        add("Sungai terpanjang di dunia adalah?",
                List.of("Amazon", "Nil", "Yangtze", "Mississippi"), 1, "Geografi");

        add("Negara dengan penduduk terbanyak di dunia adalah?",
                List.of("India", "Amerika Serikat", "Indonesia", "Cina"), 3, "Geografi");

        // --- Teknologi ---
        add("Siapa pendiri Microsoft?",
                List.of("Steve Jobs", "Bill Gates", "Mark Zuckerberg", "Elon Musk"), 1, "Teknologi");

        add("HTML adalah singkatan dari?",
                List.of("Hyper Text Markup Language", "High Tech Modern Language",
                        "Hyper Transfer Markup Link", "Home Tool Markup Language"), 0, "Teknologi");

        add("CPU adalah singkatan dari?",
                List.of("Central Processing Unit", "Computer Personal Unit",
                        "Central Program Utility", "Core Processing Unit"), 0, "Teknologi");

        add("Bahasa pemrograman apa yang digunakan untuk Android?",
                List.of("Swift", "Kotlin", "Python", "Ruby"), 1, "Teknologi");

        add("Apa kepanjangan dari RAM?",
                List.of("Read Access Memory", "Random Access Memory",
                        "Rapid Access Module", "Read And Memory"), 1, "Teknologi");

        // --- Umum ---
        add("Berapa jumlah sisi pada segitiga?",
                List.of("2", "3", "4", "5"), 1, "Umum");

        add("Warna apa yang dihasilkan dari campuran merah dan biru?",
                List.of("Hijau", "Oranye", "Ungu", "Coklat"), 2, "Umum");

        add("Berapa hari dalam seminggu?",
                List.of("5", "6", "7", "8"), 2, "Umum");

        add("Hewan apa yang dikenal sebagai raja hutan?",
                List.of("Harimau", "Singa", "Beruang", "Serigala"), 1, "Umum");

        add("Berapa jumlah bulan dalam setahun?",
                List.of("10", "11", "12", "13"), 2, "Umum");
    }

    private void add(String question, List<String> choices, int correctIndex, String category) {
        questions.add(new MultipleChoiceQuestion(question, choices, correctIndex, category));
    }

    /** Ambil soal acak */
    public QuizQuestion getRandom() {
        return questions.get(random.nextInt(questions.size()));
    }

    /** Ambil soal acak berdasarkan kategori */
    public QuizQuestion getRandomByCategory(String category) {
        List<QuizQuestion> filtered = questions.stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(category))
                .toList();
        if (filtered.isEmpty()) return getRandom();
        return filtered.get(random.nextInt(filtered.size()));
    }

    public int getTotalQuestions() {
        return questions.size();
    }
}
