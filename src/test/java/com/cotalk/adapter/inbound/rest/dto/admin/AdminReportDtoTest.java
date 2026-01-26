package com.cotalk.adapter.inbound.rest.dto.admin;

import com.cotalk.domain.entity.Report;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdminReportDto")
class AdminReportDtoTest {

    @Nested
    @DisplayName("from 메서드")
    class FromMethod {

        @Test
        @DisplayName("Report 엔티티로부터 AdminReportDto를 생성할 수 있다")
        void should_createDto_when_fromReport() {
            // given
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime processedAt = LocalDateTime.now().plusHours(1);
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(100L)
                    .reportedUserId(200L)
                    .reportedMessageId(null)
                    .reportedChatRoomId(null)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.HARASSMENT)
                    .description("욕설을 사용했습니다.")
                    .status(Report.ReportStatus.PENDING)
                    .adminNote(null)
                    .processedBy(null)
                    .processedAt(null)
                    .build();
            ReflectionTestUtils.setField(report, "createdAt", createdAt);

            // when
            AdminReportDto dto = AdminReportDto.from(report);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.reporterId()).isEqualTo(100L);
            assertThat(dto.reportedUserId()).isEqualTo(200L);
            assertThat(dto.reportedMessageId()).isNull();
            assertThat(dto.reportedChatRoomId()).isNull();
            assertThat(dto.type()).isEqualTo("USER");
            assertThat(dto.reason()).isEqualTo("HARASSMENT");
            assertThat(dto.description()).isEqualTo("욕설을 사용했습니다.");
            assertThat(dto.status()).isEqualTo("PENDING");
            assertThat(dto.adminNote()).isNull();
            assertThat(dto.processedBy()).isNull();
            assertThat(dto.processedAt()).isNull();
            assertThat(dto.createdAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("처리된 신고도 변환할 수 있다")
        void should_createDto_when_fromProcessedReport() {
            // given
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime processedAt = LocalDateTime.now().plusHours(1);
            Report report = Report.builder()
                    .id(2L)
                    .reporterId(100L)
                    .reportedMessageId(500L)
                    .reportedChatRoomId(200L)
                    .type(Report.ReportType.MESSAGE)
                    .reason(Report.ReportReason.SPAM)
                    .description("스팸 메시지입니다.")
                    .status(Report.ReportStatus.RESOLVED)
                    .adminNote("처리 완료")
                    .processedBy(1L)
                    .processedAt(processedAt)
                    .build();
            ReflectionTestUtils.setField(report, "createdAt", createdAt);

            // when
            AdminReportDto dto = AdminReportDto.from(report);

            // then
            assertThat(dto.status()).isEqualTo("RESOLVED");
            assertThat(dto.adminNote()).isEqualTo("처리 완료");
            assertThat(dto.processedBy()).isEqualTo(1L);
            assertThat(dto.processedAt()).isEqualTo(processedAt);
            assertThat(dto.type()).isEqualTo("MESSAGE");
            assertThat(dto.reason()).isEqualTo("SPAM");
        }

        @Test
        @DisplayName("report가 null인 경우 NullPointerException 발생")
        void should_throwException_when_reportIsNull() {
            // when & then
            assertThatThrownBy(() -> AdminReportDto.from(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
