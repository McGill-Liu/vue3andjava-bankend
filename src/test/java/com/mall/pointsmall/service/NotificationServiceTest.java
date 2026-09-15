package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.NotificationMessage;
import com.mall.pointsmall.enums.NotificationStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.NotificationMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    @Test
    void processedTodoCannotBeProcessedAgain() {
        NotificationMessageRepository repository = mock(NotificationMessageRepository.class);
        NotificationMessage message = new NotificationMessage();
        message.setId(1L);
        message.setStatus(NotificationStatus.PROCESSED);
        when(repository.findById(1L)).thenReturn(Optional.of(message));
        NotificationService service = new NotificationService(
                repository, mock(AdminUserRepository.class), mock(JavaMailSender.class));

        assertThrows(BusinessException.class, () -> service.process(1L, 2L));
        verify(repository, never()).save(message);
    }
}
