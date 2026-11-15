package com.example.base.data.mapper

import com.example.base.data.api.JsonPlaceholderUserDto
import com.example.base.domain.model.User

object UserMapper : Mapper<JsonPlaceholderUserDto, User> {
    override fun map(source: JsonPlaceholderUserDto): User = User(
        id = source.id.validId(),
        name = source.name.requiredText("name"),
        username = source.username.requiredText("username"),
        email = source.email.requiredText("email"),
        phone = source.phone?.trim()?.takeIf(String::isNotEmpty),
        website = source.website?.trim()?.takeIf(String::isNotEmpty)
    )

    private fun Int?.validId(): Int = this
        ?.takeIf { it > 0 }
        ?: throw DtoMappingException("JsonPlaceholderUserDto", "id", "must be positive")

    private fun String?.requiredText(field: String): String = this
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: throw DtoMappingException("JsonPlaceholderUserDto", field)
}
