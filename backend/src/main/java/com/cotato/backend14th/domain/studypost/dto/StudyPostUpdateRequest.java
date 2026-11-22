package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.global.common.request.TextDeserializer;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class StudyPostUpdateRequest {

    @Size(min = 1, max = 50)
    @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*")
    private String title;

    @Size(min = 1, max = 2000)
    @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*")
    private String content;

    private StudyStatus studyStatus;

    @JsonSetter(nulls = Nulls.FAIL)
    @JsonDeserialize(using = TextDeserializer.class)
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    @JsonDeserialize(using = TextDeserializer.class)
    public void setContent(String content) {
        this.content = content;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    @JsonDeserialize(using = TextDeserializer.class)
    public void setStudyStatus(String studyStatus) {
        this.studyStatus = StudyStatus.valueOf(studyStatus);
    }
}
