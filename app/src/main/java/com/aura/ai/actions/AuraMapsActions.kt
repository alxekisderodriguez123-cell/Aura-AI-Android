
package com.aura.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

class AuraMapsActions(private val context: Context) {

    private val mapsPackage = "com.google.android.apps.maps"

    fun navigate(destination: String): String {
        require(destination.isNotBlank())

        val uri = Uri.parse(
            "google.navigation:q=${Uri.encode(destination)}"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage(mapsPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            "Abriendo Google Maps e iniciando la ruta hacia $destination."
        } catch (_: Exception) {
            search(destination)
        }
    }

    fun search(query: String): String {
        require(query.isNotBlank())

        val uri = Uri.parse(
            "geo:0,0?q=${Uri.encode(query)}"
        )

        val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage(mapsPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(mapsIntent)
            "Buscando $query en Google Maps."
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(fallback)
            "Abriendo una aplicación de mapas para buscar $query."
        }
    }
}
