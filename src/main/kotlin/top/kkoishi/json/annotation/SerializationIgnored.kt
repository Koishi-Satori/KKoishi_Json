package top.kkoishi.json.annotation

annotation class SerializationIgnored(
    /**
     * This field is the name of method used to get the default value while serialize
     * the field annotated by this annotation.
     *
     * If getter method cannot be found or this is empty, then the field annotated by
     * this annotation will be auto-filled.
     */
    val defaultValueGetter: String = "",
)
