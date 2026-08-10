package br.rafaeros.fastrelax_api.features.collaborators;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkDayTest {

    @Test
    @DisplayName("segunda a sábado têm expediente")
    void resolvesBusinessDays() {
        // 2026-08-03 é uma segunda-feira.
        assertThat(WorkDay.from(LocalDate.of(2026, 8, 3))).contains(WorkDay.MONDAY);
        assertThat(WorkDay.from(LocalDate.of(2026, 8, 7))).contains(WorkDay.FRIDAY);
        assertThat(WorkDay.from(LocalDate.of(2026, 8, 8))).contains(WorkDay.SATURDAY);
    }

    @Test
    @DisplayName("domingo não tem expediente")
    void sundayHasNoWorkDay() {
        assertThat(WorkDay.from(LocalDate.of(2026, 8, 9))).isEmpty();
    }

    @Test
    @DisplayName("SCHEDULED e STARTED são os estados que ocupam o horário")
    void activeStatuses() {
        assertThat(SessionStatus.SCHEDULED.isActive()).isTrue();
        assertThat(SessionStatus.STARTED.isActive()).isTrue();
        assertThat(SessionStatus.DONE.isActive()).isFalse();
        assertThat(SessionStatus.EXPIRED.isActive()).isFalse();
        assertThat(SessionStatus.CANCELLED.isActive()).isFalse();
    }
}
