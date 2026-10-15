package com.example.agencia_de_vechiculos.model

/** Filtros de la HU-05. Un campo nulo o vacío no restringe la búsqueda. */
data class VehicleFilter(
    val brand: String = "",
    val model: String = "",
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val year: Int? = null,
    val fuel: FuelType? = null,
) {
    /** Regla HU-05: el precio mínimo no puede ser mayor que el máximo. */
    val hasInvalidPriceRange: Boolean
        get() = minPrice != null && maxPrice != null && minPrice > maxPrice

    fun matches(vehicle: Vehicle): Boolean =
        (brand.isBlank() || vehicle.brand.contains(brand.trim(), ignoreCase = true)) &&
            (model.isBlank() || vehicle.model.contains(model.trim(), ignoreCase = true)) &&
            (minPrice == null || vehicle.price >= minPrice) &&
            (maxPrice == null || vehicle.price <= maxPrice) &&
            (year == null || vehicle.year == year) &&
            (fuel == null || vehicle.fuel == fuel)
}
