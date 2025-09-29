package com.kopo.hanagreenworld.activity.dto;

import java.util.List;

public record QuizDataDto(
    String question,
    List<String> options,
    int correctAnswer,
    String explanation,
    int pointsReward
) {}











