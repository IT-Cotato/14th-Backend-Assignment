package com.cotato.backend14th.domain.studypost.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 작성·수정 응답 { postId }
@Getter
@AllArgsConstructor
public class StudyPostIdResponse {

    private Long postId;
}
