package com.example.base.data.mapper

/** A one-way mapper. Most remote DTOs should only expose this contract. */
fun interface Mapper<in Source, out Target> {
    fun map(source: Source): Target
}

/** Use a bidirectional mapper only when both conversions are meaningful. */
interface BiMapper<DTO, Domain> : Mapper<DTO, Domain> {
    fun toDomain(dto: DTO): Domain
    fun toDto(domain: Domain): DTO

    override fun map(source: DTO): Domain = toDomain(source)

    fun toDomainList(dtos: Iterable<DTO>): List<Domain> = dtos.map(::toDomain)
    fun toDtoList(domains: Iterable<Domain>): List<DTO> = domains.map(::toDto)
}

/**
 * Kept as a source-compatible bridge for existing local mappers.
 * New remote mappers should implement [Mapper] so they do not promise a fake reverse mapping.
 */
@Deprecated("Use Mapper or BiMapper", ReplaceWith("BiMapper<DTO, Domain>"))
interface BaseMapper<DTO, Domain> : BiMapper<DTO, Domain>

/** Raised when a syntactically valid payload cannot form a valid domain model. */
class DtoMappingException(
    val dtoName: String,
    val field: String,
    detail: String = "is missing or invalid",
    cause: Throwable? = null
) : IllegalArgumentException("$dtoName.$field $detail", cause)
