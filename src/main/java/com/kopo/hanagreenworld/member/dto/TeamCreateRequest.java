package com.kopo.hanagreenworld.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRequest {
    
    @NotBlank(message = "팀 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "팀 이름은 2-20자 사이여야 합니다.")
    private String teamName;
    
    @Size(max = 100, message = "팀 설명은 100자 이하여야 합니다.")
    private String description;
    
    @Min(value = 1, message = "최대 팀원 수는 1명 이상이어야 합니다.")
    @Max(value = 50, message = "최대 팀원 수는 50명 이하여야 합니다.")
    private Integer maxMembers = 20; // 기본값 20명
}
