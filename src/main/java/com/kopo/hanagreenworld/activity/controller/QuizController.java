package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.domain.QuizRecord;
import com.kopo.hanagreenworld.activity.dto.QuizAttemptResponse;
import com.kopo.hanagreenworld.activity.dto.QuizAttemptRequest;
import com.kopo.hanagreenworld.activity.service.QuizService;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz Controller", description = "퀴즈 관련 API")
@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "퀴즈 컨트롤러 헬스체크", description = "퀴즈 컨트롤러가 정상적으로 작동하는지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Quiz Controller is working!");
    }

    @Operation(summary = "오늘의 퀴즈 조회", description = "오늘의 환경 퀴즈를 조회합니다. 이미 참여한 경우 예외가 발생합니다.")
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<Quiz>> getDailyQuiz() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Quiz quiz = quizService.getDailyQuiz(memberId);
        return ResponseEntity.ok(ApiResponse.success("오늘의 퀴즈를 조회했습니다.", quiz));
    }

    @Operation(summary = "오늘의 퀴즈 결과 조회", description = "오늘 참여한 퀴즈의 결과를 조회합니다.")
    @GetMapping("/daily/result")
    public ResponseEntity<ApiResponse<QuizRecord>> getDailyQuizResult() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success("오늘의 퀴즈 결과를 조회했습니다.", quizService.getTodayQuizResult(memberId)));
    }

    @Operation(summary = "퀴즈 답변 제출", description = "퀴즈 답변을 제출합니다. 정답인 경우 포인트가 적립됩니다.")
    @PostMapping("/{quizId}/attempt")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> attemptQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizAttemptRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        QuizRecord result = quizService.attemptQuiz(memberId, quizId, request.getSelectedAnswer());
        QuizAttemptResponse dto = QuizAttemptResponse.builder()
                .quizId(quizId)
                .selectedAnswer(result.getSelectedAnswer())
                .isCorrect(result.getIsCorrect())
                .correctAnswer(result.getQuiz().getCorrectAnswer())
                .explanation(result.getQuiz().getExplanation())
                .pointsAwarded(result.getPointsAwarded())
                .build();
        return ResponseEntity.ok(ApiResponse.success(
            result.getIsCorrect() ? "정답입니다! 포인트가 적립되었습니다." : "아쉽네요, 다음 기회에 도전해보세요.", dto));
    }

    @Operation(summary = "퀴즈 참여 이력 조회", description = "사용자의 퀴즈 참여 이력을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<QuizRecord>>> getQuizHistory() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<QuizRecord> history = quizService.getMemberQuizHistory(memberId);
        return ResponseEntity.ok(ApiResponse.success("퀴즈 참여 이력을 조회했습니다.", history));
    }

    @Operation(summary = "오늘 퀴즈 참여 여부 확인", description = "오늘 퀴즈에 참여했는지 확인합니다.")
    @GetMapping("/daily/participation-status")
    public ResponseEntity<ApiResponse<Boolean>> getTodayQuizParticipationStatus() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Boolean hasParticipated = quizService.hasParticipatedToday(memberId);
        return ResponseEntity.ok(ApiResponse.success(
            hasParticipated ? "오늘 퀴즈에 참여했습니다." : "오늘 퀴즈에 참여하지 않았습니다.", hasParticipated));
    }
}
