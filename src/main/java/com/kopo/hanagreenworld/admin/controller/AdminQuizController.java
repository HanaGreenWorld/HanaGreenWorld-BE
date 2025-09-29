package com.kopo.hanagreenworld.admin.controller;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.service.QuizGeneratorService;
import com.kopo.hanagreenworld.activity.repository.QuizRepository;
import com.kopo.hanagreenworld.admin.dto.QuizUpdateRequest;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Quiz Controller", description = "관리자용 퀴즈 관리 API")
@RestController
@RequestMapping("/admin/quiz")
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizRepository quizRepository;

    @Operation(summary = "새 퀴즈 생성", description = "Gemini API를 사용하여 새로운 환경 퀴즈를 생성합니다.")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Quiz>> generateNewQuiz() {
        Quiz newQuiz = quizGeneratorService.generateEnvironmentQuiz();
        Quiz savedQuiz = quizRepository.save(newQuiz);
        return ResponseEntity.ok(ApiResponse.success(savedQuiz, "새로운 퀴즈가 생성되었습니다."));
    }

    @Operation(summary = "퀴즈 삭제", description = "지정된 ID의 퀴즈를 삭제합니다.")
    @DeleteMapping("/{quizId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Long quizId) {
        quizRepository.deleteById(quizId);
        return ResponseEntity.ok(ApiResponse.success(null, "퀴즈가 삭제되었습니다."));
    }

    @Operation(summary = "퀴즈 수정", description = "기존 퀴즈의 내용을 수정합니다.")
    @PutMapping("/{quizId}")
    public ResponseEntity<ApiResponse<Quiz>> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizUpdateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        Quiz updatedQuiz = Quiz.builder()
                .question(request.getQuestion())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .pointsReward(request.getPointsReward())
                .build();

        Quiz savedQuiz = quizRepository.save(updatedQuiz);
        return ResponseEntity.ok(ApiResponse.success(savedQuiz, "퀴즈가 수정되었습니다."));
    }

    @Operation(summary = "모든 퀴즈 조회", description = "등록된 모든 퀴즈를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Quiz>>> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(quizzes, "모든 퀴즈를 조회했습니다."));
    }
}

