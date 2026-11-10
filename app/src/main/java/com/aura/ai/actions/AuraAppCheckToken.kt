clipboard.setPrimaryClip(
ClipData.newPlainText(
"Aura App Check",
token
)
)
return "Listo. He copiado el token de App Check al portapapeles. Ya puedes pegarlo en Firebase."
}
private fun findToken(): String? {
val directory = File(
context.applicationInfo.dataDir,
"shared_prefs"
)
val files = directory.listFiles { file ->
file.name.endsWith(".xml") &&
file.name.contains("appcheck", ignoreCase = true)
} ?: return null
for (file in files) {
val preferences = context.getSharedPreferences(
file.name.removeSuffix(".xml"),
Context.MODE_PRIVATE
)
for (value in preferences.all.values) {
if (value is String && uuid.matches(value)) {
return value
}
}
}
return null
}
}
