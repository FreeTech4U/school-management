package com.schoolsaas.grading.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardCommentsRequest {

    @Size(max = 500, message = "Teacher comment cannot exceed 500 characters")
    private String teacherComment;

    @Size(max = 500, message = "Director comment cannot exceed 500 characters")
    private String directorComment;
}
