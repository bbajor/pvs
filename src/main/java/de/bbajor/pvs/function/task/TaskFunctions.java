package de.bbajor.pvs.function.task;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.domain.TaskRepository;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Spring Cloud Functions for Task Service.
 */
@Configuration
@RequiredArgsConstructor
public class TaskFunctions {
    
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final Clock clock;
    
    @Bean
    public Function<CompleteTaskRequest, TaskResponse> completeTask() {
        return FunctionWrapper.wrap(
            request -> {
                Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
                
                String userId = request.getSecurityContext() != null 
                    ? String.valueOf(request.getSecurityContext().getUserId()) 
                    : "SYSTEM";
                String userName = request.getSecurityContext() != null 
                    ? request.getSecurityContext().getUsername() 
                    : "SYSTEM";
                
                task.setCompleted(true);
                task.setCompletedAt(LocalDateTime.now(clock));
                task.setCompletedByUserId(userId);
                task.setCompletedByUserName(userName);
                
                Task saved = taskRepository.save(task);
                
                TaskResponse response = new TaskResponse();
                response.setTask(saved);
                response.setSuccess(true);
                return response;
            },
            "completeTask"
        );
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CompleteTaskRequest extends FunctionRequest {
        private Long taskId;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TaskResponse extends FunctionResponse {
        private Task task;
    }
}


