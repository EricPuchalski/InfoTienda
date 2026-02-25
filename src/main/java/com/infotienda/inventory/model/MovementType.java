package com.infotienda.inventory.model;

public enum MovementType {

    // ── IN ──────────────────────────────
    PURCHASE,        // Reposición de stock
    RETURN,          // Cliente devolvió un producto
    ADJUSTMENT_IN,   // Corrección manual que suma

    // ── OUT ───────────────────────────────
    SALE,            // Venta normal
    ADJUSTMENT_OUT,  // Corrección manual que resta
    LOSS,            // Pérdida física: rotura, robo

    // ── Neutros ───────────────────────────────
    INITIAL_STOCK,   // Carga inicial cuando das de alta el producto
}