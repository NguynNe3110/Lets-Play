package com.uzuu.letplay.data.mapper

import com.uzuu.letplay.data.model.TodoDto
import com.uzuu.letplay.domain.model.Todo

object TodoMapper : BiMapper<TodoDto, Todo> {
    override fun toDomain(dto: TodoDto): Todo {
        return Todo(
            id = dto.id,
            title = dto.title,
            isCompleted = dto.isCompleted
        )
    }

    override fun toDto(domain: Todo): TodoDto {
        return TodoDto(
            id = domain.id,
            title = domain.title,
            isCompleted = domain.isCompleted
        )
    }
}
